package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.OcrRemoteDataSource
import com.example.lagallens.data.datasource.remote.api.OcrApi
import com.example.lagallens.data.model.request.OcrCorrectionRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.OcrCorrectionResponse
import com.example.lagallens.data.model.response.OcrJobResponse
import com.example.lagallens.data.model.response.OcrResultResponse
import com.example.lagallens.domain.model.ocr.OcrResultNotReadyException
import com.example.lagallens.domain.model.processing.ProcessingState
import com.example.lagallens.domain.model.processing.UploadFailure
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class RemoteOcrRepositoryTest {
    private val api = OcrFixture()
    private val repository = RemoteOcrRepository(RemoteRepositorySession(), OcrRemoteDataSource(lazyOf(api)), pollIntervalMillis = 10, maxPolls = 5)

    private fun job(status: String, text: String? = null, confidence: Double? = null, retryable: Boolean = false) =
        OcrResultResponse("job-1", status, if (status == "COMPLETED") 100 else 0, null, retryable, text?.let { 1 }, text, confidence, null)

    @Test fun startsAJobAndWaitsUntilItCompletes() = runTest {
        api.results = mutableListOf(null, job("PENDING"), job("PROCESSING"), job("COMPLETED", "Hợp đồng lao động", 0.83))

        val states = repository.process("15").toList()

        assertEquals(listOf(ProcessingState.Running(stage = 3), ProcessingState.Completed), states)
        assertEquals(listOf("15"), api.createdFor)
        assertTrue(api.idempotencyKeys.single().isNotBlank())
    }

    @Test fun followsAJobThatIsAlreadyRunning() = runTest {
        api.results = mutableListOf(job("PROCESSING"), job("COMPLETED", "Văn bản", 0.9))

        val states = repository.process("15").toList()

        assertEquals(ProcessingState.Completed, states.last())
        assertTrue(api.createdFor.isEmpty())
    }

    @Test fun startsAgainAfterAFailedJob() = runTest {
        api.results = mutableListOf(job("FAILED"), job("COMPLETED", "Văn bản", 0.9))

        repository.process("15").toList()

        assertEquals(listOf("15"), api.createdFor)
    }

    @Test fun anActiveJobConflictIsFollowedInsteadOfFailing() = runTest {
        api.results = mutableListOf(null, job("COMPLETED", "Văn bản", 0.9))
        api.createFailure = httpError(409)

        assertEquals(ProcessingState.Completed, repository.process("15").toList().last())
    }

    @Test fun aFinalFailureIsReported() = runTest {
        api.results = mutableListOf(null, job("PROCESSING"), job("FAILED", retryable = false))

        assertEquals(ProcessingState.Failed(isRetryable = false), repository.process("15").toList().last())
    }

    @Test fun anUnsupportedFileCannotBeRetried() = runTest {
        api.results = mutableListOf(null)
        api.createFailure = httpError(400)

        assertEquals(
            ProcessingState.Failed(isRetryable = false, reason = UploadFailure.UNSUPPORTED_FORMAT),
            repository.process("15").toList().last()
        )
    }

    @Test fun aShortNetworkDropDoesNotStopWaiting() = runTest {
        api.results = mutableListOf(job("PROCESSING"), job("COMPLETED", "Văn bản", 0.9))
        api.networkFailuresBeforePoll = 2

        assertEquals(ProcessingState.Completed, repository.process("15").toList().last())
    }

    @Test fun stopsWaitingAfterTheTimeLimit() = runTest {
        api.results = MutableList(10) { job("PROCESSING") }

        assertEquals(ProcessingState.Failed(), repository.process("15").toList().last())
    }

    @Test fun resultCarriesTextAndConfidence() = runTest {
        api.results = mutableListOf(job("COMPLETED", "Điều 1: Quyền và nghĩa vụ", 0.834))

        val result = repository.getResult("15")

        assertEquals("Điều 1: Quyền và nghĩa vụ", result.documentText)
        assertEquals("83%", result.confidencePercentage)
        assertEquals(0.834, result.confidence!!, 0.0)
    }

    @Test fun editedResultKeepsTheOriginalConfidence() = runTest {
        api.results = mutableListOf(job("COMPLETED", "Văn bản đã sửa").copy(revision = 2, originalConfidence = 0.76))

        val result = repository.getResult("15")

        assertEquals("76%", result.confidencePercentage)
        assertEquals(0.76, result.confidence!!, 0.0)
    }

    @Test(expected = OcrResultNotReadyException::class)
    fun resultIsNotReadyBeforeTheJobCompletes() = runTest {
        api.results = mutableListOf(job("PROCESSING"))
        repository.getResult("15")
    }

    @Test(expected = OcrResultNotReadyException::class)
    fun resultIsNotReadyWhenOcrNeverRan() = runTest {
        api.results = mutableListOf(null)
        repository.getResult("15")
    }

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "{}".toResponseBody()))

    private class OcrFixture : OcrApi {
        /** Each read takes the next entry; the last one repeats. Null means no OCR job exists. */
        var results = mutableListOf<OcrResultResponse?>()
        var createFailure: Exception? = null
        var networkFailuresBeforePoll = 0
        val createdFor = mutableListOf<String>()
        val idempotencyKeys = mutableListOf<String>()
        private var reads = 0

        override suspend fun create(scope: AuthSessionScope, documentId: String, idempotencyKey: String): ApiEnvelope<OcrJobResponse> {
            createdFor += documentId
            idempotencyKeys += idempotencyKey
            createFailure?.let { throw it }
            return ApiEnvelope(true, OcrJobResponse("job-1", "PENDING"))
        }

        override suspend fun latest(scope: AuthSessionScope, documentId: String): ApiEnvelope<OcrResultResponse> {
            // The first read decides whether to start a job; drops are simulated on the polls after it.
            if (reads > 0 && networkFailuresBeforePoll > 0) {
                networkFailuresBeforePoll--
                throw IOException("offline")
            }
            val result = results[minOf(reads, results.lastIndex)]
            reads++
            return result?.let { ApiEnvelope(true, it) }
                ?: throw HttpException(Response.error<Any>(404, "{}".toResponseBody()))
        }

        override suspend fun correct(scope: AuthSessionScope, documentId: String, request: OcrCorrectionRequest) =
            ApiEnvelope(true, OcrCorrectionResponse("job-1", request.expectedRevision + 1, request.text))
    }
}
