package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.AccessSession
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import kotlinx.coroutines.flow.flowOf

internal class RemoteRepositorySession : AuthSessionStore {
    @Volatile var key = "session"

    override fun accessSession() = AccessSession(key, "fixture-token")
    override fun userId() = "42"
    override fun observeTokenChanges() = flowOf(Unit)
    override fun email() = error("Unexpected email read")
    override fun refreshToken(sessionKey: String?) = error("Unexpected refresh")
    override fun saveSession(token: String, refreshToken: String?, userId: String, email: String) =
        error("Unexpected sign-in")
    override fun replaceTokensIfRefreshMatches(oldRefresh: String, newAccess: String, newRefresh: String) =
        error("Unexpected rotation")
    override fun updateUserIfSessionMatches(sessionKey: String, userId: String, email: String) =
        error("Unexpected identity update")
    override fun clearIfSessionMatches(sessionKey: String) = error("Unexpected session clear")
    override fun clearIfRefreshMatches(expectedRefresh: String) = error("Unexpected refresh clear")
    override fun clearAccessTokenIfMatches(sessionKey: String, expectedToken: String) = error("Unexpected access clear")
}
