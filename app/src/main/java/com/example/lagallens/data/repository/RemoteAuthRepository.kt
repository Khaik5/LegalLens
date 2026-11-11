package com.example.lagallens.data.repository

import com.example.lagallens.core.network.ApiFailureMapper
import com.example.lagallens.core.network.bearerToken
import com.example.lagallens.core.network.wasRetriedAfterUnauthorized
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.remote.AuthRemoteDataSource
import com.example.lagallens.data.mapper.toApiPurpose
import com.example.lagallens.data.mapper.toAuthFailure
import com.example.lagallens.data.mapper.toOtpChallenge
import com.example.lagallens.domain.model.auth.AuthFailure
import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.model.auth.ChangePasswordResult
import com.example.lagallens.domain.model.auth.OtpChallenge
import com.example.lagallens.domain.model.auth.OtpVerification
import com.example.lagallens.domain.model.auth.OtpPurpose
import com.example.lagallens.domain.model.auth.SessionRestoreResult
import com.example.lagallens.domain.repository.AccountNotVerifiedException
import com.example.lagallens.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class RemoteAuthRepository(
    private val tokens: AuthSessionStore,
    private val remote: AuthRemoteDataSource
) : AuthRepository {
    override fun currentSessionKey(): String? = tokens.accessSession()?.key
    override fun observeSessionKeys() = tokens.observeTokenChanges().map { currentSessionKey() }.distinctUntilChanged()
    override fun currentUserId(): String? = tokens.userId()
    override fun currentUserEmail(): String? = tokens.email()

    override suspend fun restoreSession(): SessionRestoreResult = withContext(Dispatchers.IO) {
        val session = tokens.accessSession() ?: return@withContext SessionRestoreResult.NoSession
        try {
            val user = remote.me(session.key)
            val userId = ApiFailureMapper.requireBody(user.id?.takeIf(String::isNotBlank), "Current user ID")
            val email = ApiFailureMapper.requireBody(user.email?.takeIf(String::isNotBlank), "Current user email")
            if (tokens.updateUserIfSessionMatches(session.key, userId, email)) {
                SessionRestoreResult.Restored
            } else {
                sessionChanged(SessionRestoreResult.NoSession)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: HttpException) {
            if (error.code() != 401) return@withContext SessionRestoreResult.Failed(error)
            val response = error.response()?.raw()
            val refreshStillAvailable = tokens.refreshToken(session.key) != null
            if (!refreshStillAvailable || response?.wasRetriedAfterUnauthorized() == true) {
                val rejectedAccess = response?.request?.bearerToken() ?: session.token
                tokens.clearAccessTokenIfMatches(session.key, rejectedAccess)
            }
            if (tokens.accessSession() == null) SessionRestoreResult.Expired else SessionRestoreResult.Failed(error)
        } catch (error: Exception) {
            SessionRestoreResult.Failed(error)
        }
    }

    private fun sessionChanged(signedOutResult: SessionRestoreResult): SessionRestoreResult =
        if (tokens.accessSession() == null) signedOutResult
        else SessionRestoreResult.Failed(IllegalStateException("Session changed while restoring"))

    override suspend fun authenticate(username: String, password: String): Boolean {
        val response = try {
            remote.login(username, password)
        } catch (error: HttpException) {
            if (error.code() != 401) throw error
            if (ApiFailureMapper.map(error).details?.code == ACCOUNT_NOT_VERIFIED) throw AccountNotVerifiedException()
            return false
        }
        val token = ApiFailureMapper.requireBody(response.accessToken?.takeIf(String::isNotBlank), "Login access token")
        val userId = ApiFailureMapper.requireBody(response.user?.id?.takeIf(String::isNotBlank), "Login user ID")
        val email = ApiFailureMapper.requireBody(response.user?.email?.takeIf(String::isNotBlank), "Login email")
        withContext(Dispatchers.IO) { tokens.saveSession(token, response.refreshToken, userId, email) }
        return true
    }

    override suspend fun logout(): Boolean = withContext(Dispatchers.IO) {
        val session = tokens.accessSession() ?: return@withContext true
        try {
            remote.logout(session.key)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Revocation is best effort. The local sign-out must finish even while offline.
        } finally {
            withContext(NonCancellable) { tokens.clearIfSessionMatches(session.key) }
        }
        tokens.accessSession()?.key != session.key
    }

    override suspend fun register(fullName: String, email: String, password: String): AuthResult<OtpChallenge?> =
        authResult { AuthResult.Success(remote.register(fullName, email, password).toOtpChallenge()) }

    override suspend fun resendOtp(email: String, purpose: OtpPurpose): AuthResult<OtpChallenge> =
        authResult { AuthResult.Success(remote.requestChallenge(email, "RESEND", purpose.toApiPurpose()).toOtpChallenge()) }

    override suspend fun verifyOtp(challengeId: String, code: String, purpose: OtpPurpose): AuthResult<OtpVerification> =
        authResult {
            val response = remote.verifyChallenge(challengeId, code)
            when {
                !response.verified -> AuthResult.Failed(AuthFailure.INVALID_CODE)
                purpose == OtpPurpose.REGISTRATION -> AuthResult.Success(OtpVerification.Verified)
                else -> {
                    val proof = ApiFailureMapper.requireBody(
                        response.verificationToken?.takeIf(String::isNotBlank), "OTP verification token"
                    )
                    AuthResult.Success(OtpVerification.PasswordResetVerified(proof))
                }
            }
        }

    override suspend fun requestPasswordReset(email: String): AuthResult<OtpChallenge> =
        authResult { AuthResult.Success(remote.requestChallenge(email, "RESET_PASSWORD").toOtpChallenge()) }

    override suspend fun confirmPasswordReset(challengeId: String, verificationToken: String, newPassword: String): AuthResult<Unit> =
        authResult { AuthResult.Success(remote.resetPassword(challengeId, verificationToken, newPassword)) }

    private suspend fun <T> authResult(operation: suspend () -> AuthResult<T>): AuthResult<T> = try {
        operation()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        AuthResult.Failed(ApiFailureMapper.map(error).toAuthFailure())
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): ChangePasswordResult =
        withContext(Dispatchers.IO) {
            val session = tokens.accessSession() ?: return@withContext ChangePasswordResult.Failed
            try {
                remote.changePassword(session.key, currentPassword, newPassword)
                withContext(NonCancellable) {
                    tokens.clearIfSessionMatches(session.key)
                    if (tokens.accessSession() == null) ChangePasswordResult.UpdatedSignInRequired
                    else ChangePasswordResult.Failed
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val failure = ApiFailureMapper.map(error)
                when {
                    failure.statusCode == 400 && failure.details?.code == "PASSWORD_INVALID" ->
                        ChangePasswordResult.IncorrectCurrentPassword
                    failure.statusCode == 429 -> ChangePasswordResult.RateLimited
                    else -> ChangePasswordResult.Failed
                }
            }
        }

    private companion object {
        const val ACCOUNT_NOT_VERIFIED = "ACCOUNT_NOT_VERIFIED"
    }
}
