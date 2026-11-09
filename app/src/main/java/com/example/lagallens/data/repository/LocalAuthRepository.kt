package com.example.lagallens.data.repository

import android.util.Base64
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.model.auth.OtpPurpose
import com.example.lagallens.domain.repository.AuthRepository
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** DEMO authentication. No OTP is delivered, so any six-digit code passes; the check lives here so a backend replaces it. */
class LocalAuthRepository(
    private val authLocalDataSource: AuthLocalDataSource,
    private val onAccountCreated: suspend (fullName: String) -> Unit = {}
) : AuthRepository {

    override fun currentSessionKey(): String? = authLocalDataSource.currentSessionScope()?.let { "${it.userId}:${it.generation}" }

    override fun observeSessionKeys(): Flow<String?> =
        authLocalDataSource.observeSessionScope().map { scope -> scope?.let { "${it.userId}:${it.generation}" } }

    override suspend fun authenticate(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        // DEMO mode accepts validated credentials without impersonating a hardcoded account.
        if (username.isBlank() || password.isBlank()) return@withContext false
        val normalizedEmail = username.trim()
        val saved = authLocalDataSource.readPasswordCredential(normalizedEmail)
        if (saved == null) {
            if (!savePassword(normalizedEmail, password)) return@withContext false
        } else if (!verifyPassword(password, saved.first, saved.second)) return@withContext false
        authLocalDataSource.saveSession(normalizedEmail)
    }

    override fun currentUserEmail(): String? = authLocalDataSource.currentSessionEmail()

    override fun currentUserId(): String? = authLocalDataSource.currentSessionUserId()

    override suspend fun register(fullName: String, email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim()
        if (fullName.isBlank() || normalizedEmail.isBlank() || password.isBlank()) return@withContext AuthResult.REJECTED
        if (authLocalDataSource.readPasswordCredential(normalizedEmail) != null) return@withContext AuthResult.EMAIL_TAKEN
        val salt = newSalt()
        val saved = authLocalDataSource.savePendingRegistration(
            normalizedEmail, fullName.trim(),
            Base64.encodeToString(salt, Base64.NO_WRAP),
            Base64.encodeToString(derive(password, salt), Base64.NO_WRAP)
        )
        if (saved) AuthResult.SUCCESS else AuthResult.REJECTED
    }

    override suspend fun resendOtp(email: String, purpose: OtpPurpose): AuthResult = AuthResult.SUCCESS

    override suspend fun verifyOtp(email: String, code: String, purpose: OtpPurpose): AuthResult = withContext(Dispatchers.IO) {
        if (code.length != OTP_LENGTH || !code.all(Char::isDigit)) return@withContext AuthResult.INVALID_CODE
        val normalizedEmail = email.trim()
        when (purpose) {
            OtpPurpose.REGISTRATION -> completeRegistration(normalizedEmail)
            OtpPurpose.PASSWORD_RESET ->
                if (authLocalDataSource.markPasswordResetVerified(normalizedEmail)) AuthResult.SUCCESS else AuthResult.REJECTED
        }
    }

    private suspend fun completeRegistration(email: String): AuthResult {
        val pending = authLocalDataSource.readPendingRegistration(email) ?: return AuthResult.REJECTED
        if (authLocalDataSource.readPasswordCredential(email) != null) {
            authLocalDataSource.clearPendingRegistration(email)
            return AuthResult.EMAIL_TAKEN
        }
        val created = authLocalDataSource.savePasswordCredential(email, pending.saltBase64, pending.hashBase64) &&
            authLocalDataSource.saveSession(email)
        if (!created) return AuthResult.REJECTED
        authLocalDataSource.clearPendingRegistration(email)
        onAccountCreated(pending.fullName)
        return AuthResult.SUCCESS
    }

    // The result never reveals whether the email has an account.
    override suspend fun requestPasswordReset(email: String): AuthResult = AuthResult.SUCCESS

    override suspend fun confirmPasswordReset(email: String, newPassword: String): AuthResult = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim()
        if (newPassword.isBlank() || !authLocalDataSource.consumePasswordResetVerified(normalizedEmail)) return@withContext AuthResult.REJECTED
        if (authLocalDataSource.readPasswordCredential(normalizedEmail) == null) return@withContext AuthResult.REJECTED
        if (savePassword(normalizedEmail, newPassword)) AuthResult.SUCCESS else AuthResult.REJECTED
    }

    override suspend fun logout(): Boolean = withContext(Dispatchers.IO) { authLocalDataSource.clearSession() }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        val email = currentUserEmail() ?: return@withContext false
        if (currentPassword.isBlank() || newPassword.isBlank()) return@withContext false
        val existing = authLocalDataSource.readPasswordCredential(email)
        // Older DEMO sessions had no saved credential. Accept the entered current value once and migrate to a hash.
        if (existing != null && !verifyPassword(currentPassword, existing.first, existing.second)) return@withContext false
        savePassword(email, newPassword)
    }

    private fun newSalt() = ByteArray(SALT_SIZE).also(SecureRandom()::nextBytes)

    private fun savePassword(email: String, password: String): Boolean {
        val salt = newSalt()
        val hash = derive(password, salt)
        return authLocalDataSource.savePasswordCredential(email, Base64.encodeToString(salt, Base64.NO_WRAP), Base64.encodeToString(hash, Base64.NO_WRAP))
    }

    private fun verifyPassword(password: String, saltBase64: String, expectedBase64: String): Boolean = runCatching {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val expected = Base64.decode(expectedBase64, Base64.NO_WRAP)
        java.security.MessageDigest.isEqual(expected, derive(password, salt))
    }.getOrDefault(false)

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_BITS)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }

    private companion object {
        const val SALT_SIZE = 16
        const val ITERATIONS = 120_000
        const val HASH_BITS = 256
        const val OTP_LENGTH = 6
    }
}
