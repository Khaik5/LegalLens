package com.example.lagallens.data.datasource.local.datastore

import com.example.lagallens.core.network.AuthSessionScope
import kotlinx.coroutines.flow.Flow

class AccessSession(val key: String, val token: String)

interface AuthSessionStore {
    fun accessSession(): AccessSession?
    fun refreshToken(sessionKey: String? = null): String?
    fun userId(): String?
    fun email(): String?
    fun observeTokenChanges(): Flow<Unit>
    fun saveSession(token: String, refreshToken: String?, userId: String, email: String)
    fun replaceTokensIfRefreshMatches(oldRefresh: String, newAccess: String, newRefresh: String): Boolean
    fun updateUserIfSessionMatches(sessionKey: String, userId: String, email: String): Boolean
    fun clearIfSessionMatches(sessionKey: String): Boolean
    fun clearIfRefreshMatches(expectedRefresh: String): Boolean
    fun clearAccessTokenIfMatches(sessionKey: String, expectedToken: String): Boolean
}

internal fun AuthSessionStore.currentSessionScope(): AuthSessionScope? =
    accessSession()?.let { AuthSessionScope(it.key) }

internal fun AuthSessionStore.requireUserId(scope: AuthSessionScope): String {
    val userId = userId()
    check(userId != null && currentSessionScope()?.key == scope.key) { "Session changed" }
    return userId
}
