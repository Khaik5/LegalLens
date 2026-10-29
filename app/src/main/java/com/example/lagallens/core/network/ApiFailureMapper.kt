package com.example.lagallens.core.network

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

enum class ApiFailureKind {
    BACKEND_NOT_CONFIGURED,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    VALIDATION,
    RATE_LIMITED,
    SERVER,
    TIMEOUT,
    NETWORK,
    EMPTY_RESPONSE,
    OTHER
}

/** Parsed only for mapping/display decisions; callers must not log server messages or contract payloads. */
data class ApiErrorDetails(val code: String?, val message: String?, val requestId: String?)

data class ApiFailure(
    val kind: ApiFailureKind,
    val statusCode: Int? = null,
    val retryable: Boolean = false,
    val details: ApiErrorDetails? = null,
    val cause: Throwable? = null
)

class EmptyApiResponseException(feature: String) :
    IllegalStateException("$feature API returned an empty response body.")

/** Defensive transport mapping. The optional error JSON field names are PROPOSED until Backend approval. */
object ApiFailureMapper {
    fun map(throwable: Throwable): ApiFailure = when (throwable) {
        is BackendNotConfiguredException -> ApiFailure(ApiFailureKind.BACKEND_NOT_CONFIGURED, cause = throwable)
        is EmptyApiResponseException -> ApiFailure(ApiFailureKind.EMPTY_RESPONSE, cause = throwable)
        is HttpException -> mapHttp(throwable)
        is SocketTimeoutException -> ApiFailure(ApiFailureKind.TIMEOUT, retryable = true, cause = throwable)
        is IOException -> ApiFailure(ApiFailureKind.NETWORK, retryable = true, cause = throwable)
        else -> ApiFailure(ApiFailureKind.OTHER, cause = throwable)
    }

    fun <T : Any> requireBody(body: T?, feature: String): T =
        body ?: throw EmptyApiResponseException(feature)

    private fun mapHttp(error: HttpException): ApiFailure {
        val status = error.code()
        val kind = when (httpFailureKind(status)) {
            HttpFailureKind.UNAUTHORIZED -> ApiFailureKind.UNAUTHORIZED
            HttpFailureKind.FORBIDDEN -> ApiFailureKind.FORBIDDEN
            HttpFailureKind.NOT_FOUND -> ApiFailureKind.NOT_FOUND
            HttpFailureKind.CONFLICT -> ApiFailureKind.CONFLICT
            HttpFailureKind.VALIDATION -> ApiFailureKind.VALIDATION
            HttpFailureKind.RATE_LIMITED -> ApiFailureKind.RATE_LIMITED
            HttpFailureKind.SERVER -> ApiFailureKind.SERVER
            HttpFailureKind.TIMEOUT, HttpFailureKind.REQUEST_TIMEOUT -> ApiFailureKind.TIMEOUT
            HttpFailureKind.OTHER -> ApiFailureKind.OTHER
        }
        val details = runCatching {
            error.response()?.errorBody()?.use { body ->
                val json = JsonParser.parseString(body.string()).takeIf { it.isJsonObject }?.asJsonObject
                    ?: return@use null
                fun stringField(name: String) = json.get(name)?.takeIf { it.isJsonPrimitive }?.asString
                ApiErrorDetails(
                    code = stringField("code"),
                    message = stringField("message"),
                    requestId = stringField("requestId")
                )
            }
        }.getOrNull()
        val retryable = kind == ApiFailureKind.RATE_LIMITED || kind == ApiFailureKind.SERVER ||
            (kind == ApiFailureKind.TIMEOUT && status == 408)
        return ApiFailure(kind, status, retryable, details, error)
    }
}
