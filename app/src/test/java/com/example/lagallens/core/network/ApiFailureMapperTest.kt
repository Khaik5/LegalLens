package com.example.lagallens.core.network

import com.example.lagallens.data.repository.UnconfiguredRemoteAuthRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class ApiFailureMapperTest {
    @Test fun mapsRequiredHttpStatusesWithoutAssumingSuccess() {
        assertEquals(ApiFailureKind.VALIDATION, mapStatus(400).kind)
        assertEquals(ApiFailureKind.UNAUTHORIZED, mapStatus(401).kind)
        assertEquals(ApiFailureKind.FORBIDDEN, mapStatus(403).kind)
        assertEquals(ApiFailureKind.NOT_FOUND, mapStatus(404).kind)
        assertEquals(ApiFailureKind.CONFLICT, mapStatus(409).kind)
        assertEquals(ApiFailureKind.VALIDATION, mapStatus(422).kind)
        assertTrue(mapStatus(429).retryable)
        assertEquals(ApiFailureKind.SERVER, mapStatus(500).kind)
        assertTrue(mapStatus(500).retryable)
    }

    @Test fun mapsNetworkConfigurationAndEmptyBodyFailures() {
        assertEquals(ApiFailureKind.TIMEOUT, ApiFailureMapper.map(SocketTimeoutException()).kind)
        assertTrue(ApiFailureMapper.map(IOException()).retryable)
        assertEquals(ApiFailureKind.BACKEND_NOT_CONFIGURED, ApiFailureMapper.map(
            runCatching { UnconfiguredRemoteAuthRepository().authenticate("a", "b") }.exceptionOrNull()!!
        ).kind)
        assertEquals(ApiFailureKind.EMPTY_RESPONSE, ApiFailureMapper.map(EmptyApiResponseException("test")).kind)
        assertFalse(mapStatus(401).retryable)
    }

    @Test fun optionallyReadsProposedGenericErrorFields() {
        val body = """{"code":"BAD_INPUT","message":"invalid request","requestId":"req-1"}"""
            .toResponseBody("application/json".toMediaType())
        val failure = ApiFailureMapper.map(HttpException(Response.error<Any>(400, body)))
        assertEquals(ApiErrorDetails("BAD_INPUT", "invalid request", "req-1"), failure.details)
    }

    private fun mapStatus(statusCode: Int): ApiFailure = ApiFailureMapper.map(
        HttpException(Response.error<Any>(statusCode, "{}".toResponseBody("application/json".toMediaType())))
    )
}
