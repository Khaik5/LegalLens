package com.example.lagallens.data.datasource.remote

import com.example.lagallens.core.network.EmptyApiResponseException
import com.example.lagallens.data.datasource.remote.api.AuthApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthRemoteDataSourceTest {
    @Test
    fun loginReadsUserIdFromResponseData() = runTest {
        val source = dataSource(
            """{"success":true,"data":{"user":{"id":"42","email":"tester@example.test"},"accessToken":"test-token","refreshToken":"test-refresh","expiresIn":900}}"""
        )

        val response = source.login("tester@example.test", "fixture-password")

        assertEquals("42", response.user?.id)
        assertEquals("test-token", response.accessToken)
        assertEquals("test-refresh", response.refreshToken)
        assertEquals(900L, response.expiresIn)
    }

    @Test
    fun loginAcceptsResponseWithoutRefreshToken() = runTest {
        val source = dataSource(
            """{"success":true,"data":{"user":{"id":"42","email":"tester@example.test"},"accessToken":"test-token","expiresIn":900}}"""
        )

        assertNull(source.login("tester@example.test", "fixture-password").refreshToken)
    }

    @Test
    fun loginRejectsMissingData() {
        val source = dataSource("""{"success":true,"data":null}""")

        assertThrows(EmptyApiResponseException::class.java) {
            runTest { source.login("tester@example.test", "fixture-password") }
        }
    }

    private fun dataSource(json: String): AuthRemoteDataSource {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            assertEquals("POST", request.method)
            assertEquals("/authentication/login", request.url.encodedPath)
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(json.toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        val api = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
        return AuthRemoteDataSource(lazyOf(api), lazy { error("Unexpected session API call") })
    }
}
