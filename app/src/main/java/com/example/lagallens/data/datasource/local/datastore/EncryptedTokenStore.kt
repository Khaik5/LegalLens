package com.example.lagallens.data.datasource.local.datastore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores only encrypted access tokens; no token is written to logs or plain preferences. */
class EncryptedTokenStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun saveAccessToken(token: String) {
        if (token.isBlank()) {
            clear()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val packed = cipher.iv + encrypted
        check(preferences.edit().putString(KEY_ACCESS_TOKEN, Base64.encodeToString(packed, Base64.NO_WRAP)).commit())
    }

    @Synchronized
    fun accessToken(): String? = runCatching {
        val packed = preferences.getString(KEY_ACCESS_TOKEN, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return null
        require(packed.size > GCM_IV_LENGTH)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_BITS, packed.copyOfRange(0, GCM_IV_LENGTH)))
        cipher.doFinal(packed.copyOfRange(GCM_IV_LENGTH, packed.size)).toString(Charsets.UTF_8)
    }.getOrElse {
        clear()
        null
    }

    @Synchronized
    fun clear() {
        preferences.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).commit()
    }

    @Synchronized
    fun clearAccessTokenIfMatches(expectedToken: String): Boolean {
        if (accessToken() != expectedToken) return false
        return preferences.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).commit()
    }

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
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_BITS = 128
    }
}
