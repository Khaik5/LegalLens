package com.example.lagallens.data.datasource.local.datastore

import android.content.Context
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Demo-only session storage. Passwords are never persisted. */
class AuthLocalDataSource(context: Context, private val preferencesName: String = PREFERENCES_NAME) {
    private val preferences = context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    fun saveSession(userEmail: String): Boolean = synchronized(sessionLock) {
        val accountKey = "account_id_" + DemoAccountKeys.lookup(userEmail)
        val id = preferences.getString(accountKey, null) ?: UUID.randomUUID().toString()
        preferences.edit()
            .putString(accountKey, id)
            .putString(KEY_SESSION_USER_ID, id)
            .putString(KEY_SESSION_GENERATION, UUID.randomUUID().toString())
            .putString(KEY_SESSION_EMAIL, userEmail.trim())
            .putBoolean(KEY_SESSION_ACTIVE, true)
            .commit().also { if (it) notifySessionListeners() }
    }

    fun currentSessionEmail(): String? = preferences.getString(KEY_SESSION_EMAIL, null)
        ?.takeIf { preferences.getBoolean(KEY_SESSION_ACTIVE, false) }

    fun currentSessionUserId(): String? = preferences.getString(KEY_SESSION_USER_ID, null)
        ?.takeIf { preferences.getBoolean(KEY_SESSION_ACTIVE, false) }

    fun currentSessionScope(): DemoSessionScope? = synchronized(sessionLock) {
        if (!preferences.getBoolean(KEY_SESSION_ACTIVE, false)) return@synchronized null
        val id = preferences.getString(KEY_SESSION_USER_ID, null) ?: return@synchronized null
        val generation = preferences.getString(KEY_SESSION_GENERATION, null) ?: return@synchronized null
        DemoSessionScope(id, generation)
    }

    /** Serialize synchronous local mutations with login/logout. */
    internal fun <T> withSession(scope: DemoSessionScope, action: () -> T): T = synchronized(sessionLock) {
        check(currentSessionScope() == scope) { "DEMO session changed" }
        action()
    }

    internal fun addSessionListener(listener: () -> Unit) = synchronized(sessionLock) {
        sessionListeners.getOrPut(preferencesName) { mutableListOf() }.add(listener)
    }

    internal fun <T> withSessionState(action: () -> T): T = synchronized(sessionLock) { action() }

    private fun notifySessionListeners() {
        sessionListeners[preferencesName]?.toList()?.forEach { it() }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    internal fun observeSessionScope() = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(currentSessionScope())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(currentSessionScope())
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun readPasswordCredential(email: String): Pair<String, String>? {
        val prefix = "credential_" + DemoAccountKeys.lookup(email)
        val accountSalt = preferences.getString("${prefix}_salt", null)
        val accountHash = preferences.getString("${prefix}_hash", null)
        if (accountSalt != null && accountHash != null) return accountSalt to accountHash
        if (preferences.getString(KEY_PASSWORD_EMAIL, null)?.trim()?.equals(email.trim(), ignoreCase = true) != true) return null
        val salt = preferences.getString(KEY_PASSWORD_SALT, null) ?: return null
        val hash = preferences.getString(KEY_PASSWORD_HASH, null) ?: return null
        return salt to hash
    }

    fun savePasswordCredential(email: String, saltBase64: String, hashBase64: String): Boolean {
        val prefix = "credential_" + DemoAccountKeys.lookup(email)
        return preferences.edit().putString("${prefix}_salt", saltBase64)
            .putString("${prefix}_hash", hashBase64).commit()
    }

    fun clearSession(): Boolean = synchronized(sessionLock) { preferences.edit()
        .remove(KEY_SESSION_EMAIL)
        .remove(KEY_SESSION_USER_ID)
        .remove(KEY_SESSION_GENERATION)
        .putBoolean(KEY_SESSION_ACTIVE, false)
        .commit().also { if (it) notifySessionListeners() } }

    private companion object {
        val sessionLock = Any()
        val sessionListeners = mutableMapOf<String, MutableList<() -> Unit>>()
        const val PREFERENCES_NAME = "auth_demo_session"
        const val KEY_SESSION_EMAIL = "session_email"
        const val KEY_SESSION_USER_ID = "session_user_id"
        const val KEY_SESSION_GENERATION = "session_generation"
        const val KEY_SESSION_ACTIVE = "session_active"
        const val KEY_PASSWORD_EMAIL = "password_email"
        const val KEY_PASSWORD_SALT = "password_salt"
        const val KEY_PASSWORD_HASH = "password_hash"
    }
}
