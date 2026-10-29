package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.domain.repository.AuthRepository
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class LocalAuthRepository(
    private val authLocalDataSource: AuthLocalDataSource
) : AuthRepository {

    override fun authenticate(username: String, password: String): Boolean {
        // DEMO mode accepts validated credentials without impersonating a hardcoded account.
        if (username.isBlank() || password.isBlank()) return false
        val normalizedEmail = username.trim()
        val saved = authLocalDataSource.readPasswordCredential(normalizedEmail)
        if (saved == null) {
            if (!savePassword(normalizedEmail, password)) return false
        } else if (!verifyPassword(password, saved.first, saved.second)) return false
        return authLocalDataSource.saveSession(normalizedEmail)
    }

    override fun currentUserEmail(): String? = authLocalDataSource.currentSessionEmail()

    override fun currentUserId(): String? = authLocalDataSource.currentSessionUserId()

    override fun logout(): Boolean = authLocalDataSource.clearSession()

    override fun changePassword(currentPassword: String, newPassword: String): Boolean {
        val email = currentUserEmail() ?: return false
        if (currentPassword.isBlank() || newPassword.isBlank()) return false
        val existing = authLocalDataSource.readPasswordCredential(email)
        // Older DEMO sessions had no saved credential. Accept the entered current value once and migrate to a hash.
        if (existing != null && !verifyPassword(currentPassword, existing.first, existing.second)) return false
        return savePassword(email, newPassword)
    }

    private fun savePassword(email: String, password: String): Boolean {
        val salt = ByteArray(SALT_SIZE).also(SecureRandom()::nextBytes)
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
    }
}
