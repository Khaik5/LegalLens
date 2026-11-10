package com.example.lagallens.data.datasource.remote

import com.example.lagallens.data.datasource.remote.api.AuthApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthOtpRemoteDataSourceTest {
    @Test fun resendSendsOriginalPurposeAndUnwrapsAcceptedChallenge() = runTest {
        var sent: Request? = null
        val source = source { request ->
            sent = request
            response(request, 202, """{"success":true,"data":{"challengeId":"replacement","expiresIn":600,"resendAfter":60}}""")
        }

        val result = source.requestChallenge("user@example.test", "RESEND", "REGISTER")

        assertEquals("/authentication/challenges", sent?.url?.encodedPath)
        assertNull(sent?.header("Authorization"))
        val body = jsonBody(checkNotNull(sent))
        assertEquals("RESEND", body.getString("purpose"))
        assertEquals("REGISTER", body.getString("challengePurpose"))
        assertEquals("replacement", result.challengeId)
        assertEquals(60, result.resendAfter)
    }

    @Test fun passwordResetAcceptsNoContentAndSendsVerificationProof() = runTest {
        var sent: Request? = null
        val source = source { request ->
            sent = request
            response(request, 204)
        }

        source.resetPassword("test-challenge", "test-proof", "fixture-password")

        assertEquals("/authentication/password/reset", sent?.url?.encodedPath)
        assertNull(sent?.header("Authorization"))
        val body = jsonBody(checkNotNull(sent))
        assertEquals("test-challenge", body.getString("challengeId"))
        assertEquals("test-proof", body.getString("verificationToken"))
        assertEquals("fixture-password", body.getString("newPassword"))
    }

    private fun source(respond: (Request) -> Response): AuthRemoteDataSource {
        val client = OkHttpClient.Builder().addInterceptor { respond(it.request()) }.build()
        val api = Retrofit.Builder().baseUrl("http://10.0.2.2:3000/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(AuthApi::class.java)
        return AuthRemoteDataSource(lazyOf(api), lazy { error("Unexpected session API call") })
    }

    private fun response(request: Request, status: Int, json: String = "") =
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(status).message("Test response")
            .body(json.toResponseBody("application/json".toMediaType())).build()

    private fun jsonBody(request: Request): JSONObject {
        val buffer = Buffer()
        checkNotNull(request.body).writeTo(buffer)
        return JSONObject(buffer.readUtf8())
    }
}
