package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.model.auth.ChangePasswordResult
import com.example.lagallens.domain.model.auth.OtpChallenge
import com.example.lagallens.domain.model.auth.OtpPurpose
import com.example.lagallens.domain.model.auth.OtpVerification
import com.example.lagallens.domain.model.auth.SessionRestoreResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Opaque identity of the signed-in session in every data mode; null means signed out. */
    fun currentSessionKey(): String?
    fun observeSessionKeys(): Flow<String?>
    fun currentUserEmail(): String? = null
    fun currentUserId(): String? = null

    suspend fun restoreSession(): SessionRestoreResult

    /**
     * Returns false for wrong credentials. Throws [AccountNotVerifiedException] when the password is right
     * but the account's email has not been verified yet.
     */
    suspend fun authenticate(username: String, password: String): Boolean
    suspend fun register(fullName: String, email: String, password: String): AuthResult<OtpChallenge?>
    suspend fun resendOtp(email: String, purpose: OtpPurpose): AuthResult<OtpChallenge>
    suspend fun verifyOtp(challengeId: String, code: String, purpose: OtpPurpose): AuthResult<OtpVerification>
    suspend fun requestPasswordReset(email: String): AuthResult<OtpChallenge>
    suspend fun confirmPasswordReset(challengeId: String, verificationToken: String, newPassword: String): AuthResult<Unit>
    suspend fun logout(): Boolean
    suspend fun changePassword(currentPassword: String, newPassword: String): ChangePasswordResult
}

/** The account exists and the password is right, but its email must be verified before signing in. */
class AccountNotVerifiedException : Exception("Account email verification is required")
