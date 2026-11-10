package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthHeaderInterceptor
import com.example.lagallens.core.network.TokenRefreshAuthenticator
import com.example.lagallens.data.datasource.local.datastore.AccessSession
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.remote.AuthRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.AuthApi
import com.example.lagallens.data.datasource.remote.api.AuthSessionApi
import com.example.lagallens.data.datasource.remote.rotateTokens
import com.example.lagallens.domain.model.auth.ChangePasswordResult
import com.example.lagallens.domain.model.auth.SessionRestoreResult
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RemoteAuthRepositoryTest {
    @Test fun restoresIdentityFromCurrentUserEndpoint() = runTest {
        val store = TestSessionStore()
        var sentRequest: Request? = null
        val repository = stubRepository(store) { request ->
            sentRequest = request
            response(request, 200, CURRENT_USER)
        }

        assertEquals(SessionRestoreResult.Restored, repository.restoreSession())
        assertEquals("GET", sentRequest?.method)
        assertEquals("/authentication/me", sentRequest?.url?.encodedPath)
        assertEquals("Bearer test-access", sentRequest?.header("Authorization"))
        assertEquals("42", repository.currentUserId())
        assertEquals("current@example.test", repository.currentUserEmail())
        assertEquals("test-access", store.accessSession()?.token)
        assertEquals("test-refresh", store.refreshToken())
    }

    @Test fun unauthorizedWithNoRefreshClearsLegacySession() = runTest {
        val store = TestSessionStore(refresh = null)
        val repository = stubRepository(store) { response(it, 401) }

        assertEquals(SessionRestoreResult.Expired, repository.restoreSession())
        assertNull(store.accessSession())
        assertNull(repository.currentUserId())
    }

    @Test fun connectionFailurePreservesSession() = runTest {
        val store = TestSessionStore()
        val repository = stubRepository(store) { throw IOException("Connection unavailable") }

        val result = repository.restoreSession()

        assertTrue(result is SessionRestoreResult.Failed && result.cause is IOException)
        assertEquals("test-access", store.accessSession()?.token)
        assertEquals("test-refresh", store.refreshToken())
    }

    @Test fun serverFailurePreservesSession() = runTest {
        val store = TestSessionStore()
        val repository = stubRepository(store) { response(it, 503) }

        assertTrue(repository.restoreSession() is SessionRestoreResult.Failed)
        assertEquals("test-access", store.accessSession()?.token)
    }

    @Test fun missingTokenSkipsNetwork() = runTest {
        val store = TestSessionStore().apply { clear() }
        var called = false
        val repository = stubRepository(store) {
            called = true
            response(it, 401)
        }

        assertEquals(SessionRestoreResult.NoSession, repository.restoreSession())
        assertEquals(false, called)
    }

    @Test fun lateUnauthorizedResponseDoesNotClearNewLogin() = runTest {
        val store = TestSessionStore()
        val repository = stubRepository(store) {
            store.saveSession("new-access", "new-refresh", "99", "new@example.test")
            response(it, 401)
        }

        assertTrue(repository.restoreSession() is SessionRestoreResult.Failed)
        assertEquals("new-access", store.accessSession()?.token)
        assertEquals("new-refresh", store.refreshToken())
        assertEquals("99", repository.currentUserId())
    }

    @Test fun concurrentUnauthorizedRequestsShareRotationAndKeepSessionIdentity() = runTest {
        val store = TestSessionStore()
        val originalKey = store.accessSession()?.key
        val initialRequests = CountDownLatch(2)
        val sent = ConcurrentLinkedQueue<Received>()
        withServer(store, { request ->
            sent.add(request)
            when {
                request.path == "/authentication/refresh" -> Reply(200, ROTATED)
                request.authorization == "Bearer renewed-access" -> Reply(200, CURRENT_USER)
                else -> {
                    initialRequests.countDown()
                    if (initialRequests.await(5, TimeUnit.SECONDS)) Reply(401) else Reply(503)
                }
            }
        }) { repository ->
            val first = async { repository.restoreSession() }
            val second = async { repository.restoreSession() }
            assertEquals(SessionRestoreResult.Restored, first.await())
            assertEquals(SessionRestoreResult.Restored, second.await())
        }

        val refresh = sent.single { it.path == "/authentication/refresh" }
        assertEquals("POST", refresh.method)
        assertNull(refresh.authorization)
        assertEquals("test-refresh", JSONObject(refresh.body).getString("refreshToken"))
        assertEquals(2, sent.count { it.authorization == "Bearer renewed-access" })
        assertEquals(originalKey, store.accessSession()?.key)
        assertEquals("renewed-refresh", store.refreshToken())
        assertEquals("42", store.userId())
    }

    @Test fun unavailableRefreshPreservesSessionAndReturnsRetryableRestore() = runTest {
        val store = TestSessionStore()
        withServer(store, { if (it.path.endsWith("/refresh")) Reply(503) else Reply(401) }) { repository ->
            assertTrue(repository.restoreSession() is SessionRestoreResult.Failed)
        }
        assertEquals("test-access", store.accessSession()?.token)
        assertEquals("test-refresh", store.refreshToken())
    }

    @Test fun networkFailureDuringRefreshPreservesSession() = runTest {
        val store = TestSessionStore()
        withServer(
            store,
            { Reply(401) },
            refreshTransport = { throw IOException("Offline") }
        ) { repository ->
            assertTrue(repository.restoreSession() is SessionRestoreResult.Failed)
        }
        assertEquals("test-refresh", store.refreshToken())
    }

    @Test fun rejectedRefreshClearsSession() = runTest {
        val store = TestSessionStore()
        withServer(store, { Reply(401) }) { repository ->
            assertEquals(SessionRestoreResult.Expired, repository.restoreSession())
        }
        assertNull(store.accessSession())
        assertNull(store.refreshToken())
    }

    @Test fun rejectedRefreshFromOldLoginCannotClearReplacementSession() = runTest {
        val store = TestSessionStore()
        withServer(store, {
            if (it.path.endsWith("/refresh")) {
                store.saveSession("new-access", "new-refresh", "99", "new@example.test")
            }
            Reply(401)
        }) { repository ->
            assertTrue(repository.restoreSession() is SessionRestoreResult.Failed)
        }
        assertEquals("new-access", store.accessSession()?.token)
        assertEquals("new-refresh", store.refreshToken())
        assertEquals("99", store.userId())
    }

    @Test fun retriesProtectedRequestOnlyOnce() = runTest {
        val store = TestSessionStore()
        val sent = ConcurrentLinkedQueue<Received>()
        withServer(store, {
            sent.add(it)
            if (it.path.endsWith("/refresh")) Reply(200, ROTATED) else Reply(401)
        }) { repository ->
            assertEquals(SessionRestoreResult.Expired, repository.restoreSession())
        }
        assertEquals(1, sent.count { it.path.endsWith("/refresh") })
        assertEquals(2, sent.count { it.path.endsWith("/me") })
    }

    @Test fun logoutCallsBackendAndClearsLocalSessionOnFailure() = runTest {
        for (status in listOf(204, 401, 503)) {
            val store = TestSessionStore()
            var sent: Request? = null
            val repository = stubRepository(store) {
                sent = it
                response(it, status)
            }
            assertTrue(repository.logout())
            assertEquals("POST", sent?.method)
            assertEquals("/authentication/logout", sent?.url?.encodedPath)
            assertEquals("Bearer test-access", sent?.header("Authorization"))
            assertNull(store.accessSession())
            assertNull(store.refreshToken())
        }
    }

    @Test fun logoutClearsLocalSessionWhileOffline() = runTest {
        val store = TestSessionStore()
        val repository = stubRepository(store) { throw IOException("Offline") }

        assertTrue(repository.logout())
        assertNull(store.accessSession())
    }

    @Test fun changePasswordSendsBothPasswordsAndSignsOutOnNoContent() = runTest {
        val store = TestSessionStore()
        val sent = ConcurrentLinkedQueue<Received>()
        withServer(store, { sent.add(it); Reply(204) }) { repository ->
            assertEquals(ChangePasswordResult.UpdatedSignInRequired, repository.changePassword("fixture-old", "fixture-new"))
        }
        val request = sent.single()
        assertEquals("PATCH", request.method)
        assertEquals("/authentication/password", request.path)
        assertEquals("Bearer test-access", request.authorization)
        val body = JSONObject(request.body)
        assertEquals("fixture-old", body.getString("currentPassword"))
        assertEquals("fixture-new", body.getString("newPassword"))
        assertNull(store.accessSession())
        assertNull(store.refreshToken())
    }

    @Test fun changePasswordDistinguishesWrongPasswordAndRateLimit() = runTest {
        val store = TestSessionStore()
        val incorrect = stubRepository(store) { response(it, 400, """{"code":"PASSWORD_INVALID"}""") }
        val limited = stubRepository(store) { response(it, 429, """{"code":"RATE_LIMITED"}""") }

        assertEquals(ChangePasswordResult.IncorrectCurrentPassword, incorrect.changePassword("fixture-old", "fixture-new"))
        assertEquals(ChangePasswordResult.RateLimited, limited.changePassword("fixture-old", "fixture-new"))
        assertEquals("test-access", store.accessSession()?.token)
    }

    private fun stubRepository(store: TestSessionStore, respond: (Request) -> Response): RemoteAuthRepository {
        val client = OkHttpClient.Builder().addInterceptor(AuthHeaderInterceptor(store))
            .addInterceptor { respond(it.request()) }.build()
        return RemoteAuthRepository(store, AuthRemoteDataSource(
            api = lazy { error("Unexpected public API call") },
            sessionApi = lazy { retrofit("http://10.0.2.2:3000/", client).create(AuthSessionApi::class.java) }
        ))
    }

    private suspend fun withServer(
        store: TestSessionStore,
        respond: (Received) -> Reply,
        refreshTransport: ((Request) -> Response)? = null,
        block: suspend (RemoteAuthRepository) -> Unit
    ) {
        val executor = Executors.newCachedThreadPool()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = executor
        server.createContext("/") { exchange ->
            exchange.use {
                val request = Received(
                    exchange.requestMethod,
                    exchange.requestURI.path,
                    exchange.requestHeaders.getFirst("Authorization"),
                    exchange.requestBody.bufferedReader().readText()
                )
                val reply = respond(request)
                val bytes = reply.json.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.set("Content-Type", "application/json")
                exchange.sendResponseHeaders(reply.status, if (reply.status == 204) -1L else bytes.size.toLong())
                if (reply.status != 204) exchange.responseBody.write(bytes)
            }
        }
        server.start()

        val baseUrl = "http://127.0.0.1:${server.address.port}/"
        val publicClient = OkHttpClient.Builder().retryOnConnectionFailure(false).apply {
            if (refreshTransport != null) addInterceptor { refreshTransport(it.request()) }
        }.build()
        val api = retrofit(baseUrl, publicClient).create(AuthApi::class.java)
        val sessionClient = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor(store))
            .authenticator(TokenRefreshAuthenticator(store, api::rotateTokens))
            .build()
        val sessionApi = retrofit(baseUrl, sessionClient).create(AuthSessionApi::class.java)

        try {
            block(RemoteAuthRepository(store, AuthRemoteDataSource(lazyOf(api), lazyOf(sessionApi))))
        } finally {
            server.stop(0)
            executor.shutdownNow()
            for (client in listOf(publicClient, sessionClient)) {
                client.connectionPool.evictAll()
                client.dispatcher.executorService.shutdown()
            }
        }
    }

    private fun retrofit(url: String, client: OkHttpClient) = Retrofit.Builder()
        .baseUrl(url).client(client).addConverterFactory(GsonConverterFactory.create()).build()

    private fun response(request: Request, code: Int, body: String = if (code == 204) "" else "{}") =
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("Test response")
            .body(body.toResponseBody("application/json".toMediaType())).build()

    private data class Received(val method: String, val path: String, val authorization: String?, val body: String)
    private data class Reply(val status: Int, val json: String = "{}")

    private class TestSessionStore(private var refresh: String? = "test-refresh") : AuthSessionStore {
        private var session: AccessSession? = AccessSession(UUID.randomUUID().toString(), "test-access")
        private var id: String? = "1"
        private var address: String? = "previous@example.test"

        @Synchronized override fun accessSession() = session
        @Synchronized override fun refreshToken(sessionKey: String?) =
            refresh.takeIf { sessionKey == null || session?.key == sessionKey }
        @Synchronized override fun userId() = id
        @Synchronized override fun email() = address
        override fun observeTokenChanges() = flowOf(Unit)

        @Synchronized override fun saveSession(token: String, refreshToken: String?, userId: String, email: String) {
            session = AccessSession(UUID.randomUUID().toString(), token)
            refresh = refreshToken
            id = userId
            address = email
        }

        @Synchronized override fun replaceTokensIfRefreshMatches(oldRefresh: String, newAccess: String, newRefresh: String): Boolean {
            val current = session ?: return false
            if (refresh != oldRefresh) return false
            session = AccessSession(current.key, newAccess)
            refresh = newRefresh
            return true
        }

        @Synchronized override fun updateUserIfSessionMatches(sessionKey: String, userId: String, email: String): Boolean {
            if (session?.key != sessionKey) return false
            id = userId
            address = email
            return true
        }

        @Synchronized override fun clearIfSessionMatches(sessionKey: String): Boolean =
            session?.key == sessionKey && clear()

        @Synchronized override fun clearIfRefreshMatches(expectedRefresh: String): Boolean =
            refresh == expectedRefresh && clear()

        @Synchronized override fun clearAccessTokenIfMatches(sessionKey: String, expectedToken: String): Boolean =
            session?.let { it.key == sessionKey && it.token == expectedToken } == true && clear()

        @Synchronized fun clear(): Boolean {
            session = null
            refresh = null
            id = null
            address = null
            return true
        }
    }

    private companion object {
        const val CURRENT_USER = """{"success":true,"data":{"id":"42","email":"current@example.test","fullName":"Test User"}}"""
        const val ROTATED = """{"success":true,"data":{"accessToken":"renewed-access","refreshToken":"renewed-refresh","expiresIn":900}}"""
    }
}
