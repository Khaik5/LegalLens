package com.example.lagallens.data.datasource.remote

import com.example.lagallens.data.datasource.remote.api.AuthApi
import com.example.lagallens.data.datasource.remote.api.AuthSessionApi
import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.core.network.EmptyApiResponseException
import com.example.lagallens.core.network.TokenRefreshResult
import com.example.lagallens.data.model.request.ChallengeRequest
import com.example.lagallens.data.model.request.ChangePasswordRequest
import com.example.lagallens.data.model.request.RefreshTokenRequest
import com.example.lagallens.data.model.request.LoginRequest
import com.example.lagallens.data.model.request.RegisterRequest
import com.example.lagallens.data.model.request.ResetPasswordRequest
import com.example.lagallens.data.model.request.VerifyChallengeRequest
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AuthRemoteDataSource(
    private val api: Lazy<AuthApi>,
    private val sessionApi: Lazy<AuthSessionApi>
) {
    suspend fun login(email: String, password: String) =
        api.value.login(LoginRequest(email, password)).requireData("Login")

    suspend fun me(sessionKey: String) =
        sessionApi.value.me(AuthSessionScope(sessionKey)).requireData("Current user")

    suspend fun logout(sessionKey: String) =
        sessionApi.value.logout(AuthSessionScope(sessionKey)).requireNoContent("Logout")

    suspend fun changePassword(sessionKey: String, currentPassword: String, newPassword: String) =
        sessionApi.value.changePassword(
            AuthSessionScope(sessionKey), ChangePasswordRequest(currentPassword, newPassword)
        ).requireNoContent("Password change")

    suspend fun register(fullName: String, email: String, password: String) =
        api.value.register(RegisterRequest(fullName, email, password)).requireData("Registration")

    suspend fun requestChallenge(email: String, purpose: String, challengePurpose: String? = null) =
        api.value.requestChallenge(ChallengeRequest(purpose, email, challengePurpose)).requireData("OTP challenge")

    suspend fun verifyChallenge(challengeId: String, code: String) =
        api.value.verifyChallenge(VerifyChallengeRequest(challengeId, code)).requireData("OTP verification")

    suspend fun resetPassword(challengeId: String, verificationToken: String, newPassword: String) {
        api.value.resetPassword(ResetPasswordRequest(challengeId, verificationToken, newPassword))
            .requireNoContent("Password reset")
    }

    private fun Response<Unit>.requireNoContent(feature: String) {
        if (!isSuccessful) throw HttpException(this)
        if (code() != 204) throw EmptyApiResponseException(feature)
    }
}

internal fun AuthApi.rotateTokens(refreshToken: String): TokenRefreshResult {
    val response = try {
        // This synchronous call uses the public client, so it cannot queue behind or reenter the authenticator.
        refresh(RefreshTokenRequest(refreshToken)).execute()
    } catch (_: IOException) {
        return TokenRefreshResult.Unavailable
    } catch (_: RuntimeException) {
        return TokenRefreshResult.Unavailable
    }
    response.errorBody()?.close()
    if (response.code() == 401) return TokenRefreshResult.Rejected
    val pair = response.body()?.takeIf { it.success }?.data ?: return TokenRefreshResult.Unavailable
    val access = pair.accessToken?.takeIf(String::isNotBlank) ?: return TokenRefreshResult.Unavailable
    val refresh = pair.refreshToken?.takeIf(String::isNotBlank) ?: return TokenRefreshResult.Unavailable
    return TokenRefreshResult.Rotated(access, refresh)
}
