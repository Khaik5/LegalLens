package com.example.lagallens.data.mapper

import com.example.lagallens.core.network.ApiFailureMapper
import com.example.lagallens.core.network.EmptyApiResponseException
import com.example.lagallens.data.model.response.RegisterResponse
import com.example.lagallens.domain.model.auth.AuthFailure
import com.example.lagallens.domain.model.auth.OtpChallenge
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AuthOtpMapperTest {
    private val registration = RegisterResponse("42", "user@example.test", "Test User", null, null, null, null)

    @Test fun activeRegistrationWithoutChallengeCanGoToLogin() {
        assertNull(registration.toOtpChallenge())
    }

    @Test fun pendingRegistrationMapsServerChallengeAndTiming() {
        val pending = registration.copy(
            accountStatus = "PENDING_VERIFICATION",
            challengeId = "test-challenge",
            expiresIn = 600,
            resendAfter = 60
        )
        assertEquals(OtpChallenge("test-challenge", 600, 60), pending.toOtpChallenge())
    }

    @Test fun pendingRegistrationWithoutChallengeCannotGoToLogin() {
        assertThrows(EmptyApiResponseException::class.java) {
            registration.copy(accountStatus = "PENDING_VERIFICATION").toOtpChallenge()
        }
    }

    @Test fun mapsOtpAndAuthErrorsFromBackendEnvelope() {
        assertEquals(AuthFailure.INVALID_CODE, failure(400, "OTP_INVALID"))
        assertEquals(AuthFailure.OTP_RATE_LIMITED, failure(429, "OTP_RATE_LIMITED"))
        assertEquals(AuthFailure.RATE_LIMITED, failure(429, "RATE_LIMITED"))
        assertEquals(AuthFailure.EMAIL_TAKEN, failure(409, "EMAIL_EXISTS"))
        assertEquals(AuthFailure.EMAIL_TAKEN, failure(409, "CONFLICT"))
        assertEquals(AuthFailure.PHASE_DISABLED, failure(503, "AUTH_PHASE02_DISABLED"))
    }

    private fun failure(status: Int, code: String): AuthFailure =
        ApiFailureMapper.map(HttpException(Response.error<Unit>(status, """{"code":"$code"}""".toResponseBody())))
            .toAuthFailure()
}
