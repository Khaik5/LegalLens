package com.example.lagallens.data.datasource.local.datastore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.channels.awaitClose

/** Persists the user identity and its encrypted tokens as one session. */
class EncryptedTokenStore(context: Context) : AuthSessionStore {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** Allows observers to derive the current session after credentials or identity change. */
    override fun observeTokenChanges(): kotlinx.coroutines.flow.Flow<Unit> = kotlinx.coroutines.flow.callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(Unit) }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(Unit)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override fun saveSession(token: String, refreshToken: String?, userId: String, email: String) = synchronized(sessionLock) {
        require(token.isNotBlank() && userId.isNotBlank() && email.isNotBlank())
        val encryptedRefreshToken = refreshToken?.takeIf(String::isNotBlank)?.let(::encryptToken)
        check(
            preferences.edit()
                .putString(KEY_ACCESS_TOKEN, encryptToken(token))
                .putString(KEY_REFRESH_TOKEN, encryptedRefreshToken)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_EMAIL, email)
                .putString(KEY_SESSION_ID, UUID.randomUUID().toString())
                .commit()
        )
    }

    private fun encryptToken(token: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    override fun userId(): String? = sessionValue(KEY_USER_ID)

    override fun email(): String? = sessionValue(KEY_EMAIL)

    override fun updateUserIfSessionMatches(sessionKey: String, userId: String, email: String): Boolean =
        synchronized(sessionLock) {
            if (readAccessSession()?.key != sessionKey) return@synchronized false
            check(preferences.edit().putString(KEY_USER_ID, userId).putString(KEY_EMAIL, email).commit())
            true
        }

    private fun sessionValue(key: String): String? = synchronized(sessionLock) {
        if (readAccessSession() == null) null else preferences.getString(key, null)
    }

    override fun accessSession(): AccessSession? = synchronized(sessionLock) { readAccessSession() }

    override fun refreshToken(sessionKey: String?): String? = synchronized(sessionLock) {
        if (sessionKey != null && readAccessSession()?.key != sessionKey) return@synchronized null
        readToken(KEY_REFRESH_TOKEN)
    }

    override fun replaceTokensIfRefreshMatches(oldRefresh: String, newAccess: String, newRefresh: String): Boolean =
        synchronized(sessionLock) {
            val current = readAccessSession() ?: return@synchronized false
            if (readToken(KEY_REFRESH_TOKEN) != oldRefresh) return@synchronized false
            check(
                preferences.edit()
                    .putString(KEY_ACCESS_TOKEN, encryptToken(newAccess))
                    .putString(KEY_REFRESH_TOKEN, encryptToken(newRefresh))
                    .putString(KEY_SESSION_ID, current.key)
                    .commit()
            )
            true
        }

    private fun readAccessSession(): AccessSession? {
        val token = readToken(KEY_ACCESS_TOKEN)
        if (token == null) {
            if (preferences.contains(KEY_ACCESS_TOKEN)) clearSession()
            return null
        }
        // Older installs derived session identity from the access token.
        val key = preferences.getString(KEY_SESSION_ID, null) ?: MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return AccessSession(key, token)
    }

    private fun readToken(preference: String): String? = runCatching {
        val packed = preferences.getString(preference, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        require(packed.size > GCM_IV_LENGTH)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_BITS, packed.copyOfRange(0, GCM_IV_LENGTH)))
        cipher.doFinal(packed.copyOfRange(GCM_IV_LENGTH, packed.size)).toString(Charsets.UTF_8)
    }.getOrNull()

    override fun clearIfSessionMatches(sessionKey: String): Boolean = synchronized(sessionLock) {
        readAccessSession()?.key == sessionKey && clearSession()
    }

    override fun clearIfRefreshMatches(expectedRefresh: String): Boolean = synchronized(sessionLock) {
        readToken(KEY_REFRESH_TOKEN) == expectedRefresh && clearSession()
    }

    override fun clearAccessTokenIfMatches(sessionKey: String, expectedToken: String): Boolean = synchronized(sessionLock) {
        val current = readAccessSession()
        current?.key == sessionKey && current.token == expectedToken && clearSession()
    }

    private fun clearSession(): Boolean = preferences.edit()
        .remove(KEY_ACCESS_TOKEN)
        .remove(KEY_REFRESH_TOKEN)
        .remove(KEY_USER_ID)
        .remove(KEY_EMAIL)
        .remove(KEY_SESSION_ID)
        .commit()

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "legallens_access_token"
        const val PREFERENCES_NAME = "secure_auth_tokens"
        const val KEY_ACCESS_TOKEN = "access_token_encrypted"
        const val KEY_REFRESH_TOKEN = "refresh_token_encrypted"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "user_email"
        const val KEY_SESSION_ID = "session_id"
        val sessionLock = Any()
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_BITS = 128
    }
}
