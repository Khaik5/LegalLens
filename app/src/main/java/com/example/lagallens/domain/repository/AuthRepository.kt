package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.model.auth.OtpPurpose
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Opaque identity of the signed-in session in every data mode; null means signed out. */
    fun currentSessionKey(): String?
    fun observeSessionKeys(): Flow<String?>
    fun currentUserEmail(): String? = null
    fun currentUserId(): String? = null

    suspend fun authenticate(username: String, password: String): Boolean
    suspend fun register(fullName: String, email: String, password: String): AuthResult
    suspend fun resendOtp(email: String, purpose: OtpPurpose): AuthResult
    suspend fun verifyOtp(email: String, code: String, purpose: OtpPurpose): AuthResult
    suspend fun requestPasswordReset(email: String): AuthResult
    suspend fun confirmPasswordReset(email: String, newPassword: String): AuthResult
    suspend fun logout(): Boolean
    suspend fun changePassword(currentPassword: String, newPassword: String): Boolean
}
