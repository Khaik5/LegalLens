package com.example.lagallens.core.network

import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

class AuthSessionScope(val key: String)

sealed interface TokenRefreshResult {
    class Rotated(val accessToken: String, val refreshToken: String) : TokenRefreshResult
    data object Rejected : TokenRefreshResult
    data object Unavailable : TokenRefreshResult
}

class TokenRefreshAuthenticator(
    private val tokens: AuthSessionStore,
    private val refresh: (String) -> TokenRefreshResult
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        val attempted = request.bearerToken() ?: return null
        val scope = request.tag(AuthSessionScope::class.java) ?: return null
        if (response.wasRetriedAfterUnauthorized()) return null

        val flight = synchronized(refreshLock) {
            val current = tokens.accessSession()?.takeIf { it.key == scope.key } ?: return null
            if (current.token != attempted) return request.withBearer(current.token)
            refreshFlights.getOrPut(scope.key) {
                FutureTask { rotate(scope.key, attempted) }
            }
        }
        // FutureTask runs once. Other callers wait for the same rotation, including on failure.
        flight.run()
        try {
            flight.get()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            return null
        } catch (_: ExecutionException) {
            return null
        } finally {
            synchronized(refreshLock) {
                if (flight.isDone && refreshFlights[scope.key] === flight) refreshFlights.remove(scope.key)
            }
        }

        val updated = tokens.accessSession()?.takeIf { it.key == scope.key && it.token != attempted }
        return updated?.let { request.withBearer(it.token) }
    }

    private fun rotate(sessionKey: String, attempted: String) {
        val oldRefresh = tokens.refreshToken(sessionKey)
        if (oldRefresh == null) {
            tokens.clearAccessTokenIfMatches(sessionKey, attempted)
            return
        }
        when (val result = refresh(oldRefresh)) {
            TokenRefreshResult.Rejected -> tokens.clearIfRefreshMatches(oldRefresh)
            TokenRefreshResult.Unavailable -> Unit
            is TokenRefreshResult.Rotated ->
                tokens.replaceTokensIfRefreshMatches(oldRefresh, result.accessToken, result.refreshToken)
        }
    }

    private companion object {
        val refreshLock = Any()
        val refreshFlights = mutableMapOf<String, FutureTask<Unit>>()
    }
}

internal fun Request.bearerToken(): String? = header("Authorization")
    ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
    ?.substring(7)
    ?.takeIf(String::isNotBlank)

internal fun Request.withBearer(token: String): Request = newBuilder().header("Authorization", "Bearer $token").build()

internal fun Response.wasRetriedAfterUnauthorized(): Boolean =
    generateSequence(priorResponse) { it.priorResponse }.any { it.code == 401 }
