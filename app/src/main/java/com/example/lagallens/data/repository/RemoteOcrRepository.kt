package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.remote.OcrRemoteDataSource
import com.example.lagallens.data.model.response.OcrResultResponse
import com.example.lagallens.domain.model.ocr.OcrResult
import com.example.lagallens.domain.model.ocr.OcrResultNotReadyException
import com.example.lagallens.domain.model.processing.ProcessingState
import com.example.lagallens.domain.model.processing.UploadFailure
import com.example.lagallens.domain.repository.OcrRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Runs OCR on the backend for an uploaded document. The backend recognizes the whole file at once,
 * so the job only reports waiting, recognizing and finished.
 */
class RemoteOcrRepository(
    private val tokens: AuthSessionStore,
    private val remote: OcrRemoteDataSource,
    private val pollIntervalMillis: Long = POLL_INTERVAL_MILLIS,
    private val maxPolls: Int = MAX_POLLS
) : OcrRepository {
    override fun process(documentId: String): Flow<ProcessingState> = flow {
        val scope = tokens.currentSessionScope()?.let { AuthSessionScope(it.key) }
        if (scope == null) {
            emit(ProcessingState.Failed())
            return@flow
        }
        try {
            val existing = remote.latest(scope, documentId)
            if (existing == null || existing.status in FINISHED_WITHOUT_TEXT) startJob(scope, documentId)
            emit(ProcessingState.Running(stage = RECOGNIZING_STAGE))
            emit(awaitJob(scope, documentId))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: HttpException) {
            error.response()?.errorBody()?.close()
            emit(ProcessingState.Failed(
                isRetryable = error.code() != 400,
                reason = if (error.code() == 400) UploadFailure.UNSUPPORTED_FORMAT else UploadFailure.UNKNOWN
            ))
        } catch (_: IOException) {
            emit(ProcessingState.Failed(reason = UploadFailure.NETWORK))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getResult(documentId: String): OcrResult = withContext(Dispatchers.IO) {
        val scope = AuthSessionScope(checkNotNull(tokens.currentSessionScope()) { "No active session" }.key)
        val result = remote.latest(scope, documentId)
        if (result?.status != COMPLETED) throw OcrResultNotReadyException()
        check(tokens.currentSessionScope()?.key == scope.key) { "Session changed" }
        // Edited text keeps the confidence of the recognition it started from.
        val confidence = result.originalConfidence ?: result.confidence
        OcrResult(
            confidenceLabel = "",
            confidencePercentage = confidence?.let { "${(it * 100).roundToInt()}%" } ?: "—",
            documentText = result.text.orEmpty(),
            confidence = confidence
        )
    }

    private suspend fun startJob(scope: AuthSessionScope, documentId: String) {
        try {
            remote.create(scope, documentId, UUID.randomUUID().toString())
        } catch (error: HttpException) {
            // Another request already started recognition; follow that job instead.
            if (error.code() != 409) throw error
            error.response()?.errorBody()?.close()
        }
    }

    /** A few network drops are tolerated because recognition keeps running on the backend. */
    private suspend fun awaitJob(scope: AuthSessionScope, documentId: String): ProcessingState {
        var networkFailures = 0
        repeat(maxPolls) {
            delay(pollIntervalMillis)
            val job: OcrResultResponse? = try {
                remote.latest(scope, documentId).also { networkFailures = 0 }
            } catch (error: IOException) {
                if (++networkFailures > MAX_NETWORK_FAILURES) throw error
                return@repeat
            }
            when (job?.status) {
                COMPLETED -> return ProcessingState.Completed
                FAILED -> return ProcessingState.Failed(isRetryable = job.retryable != false)
                CANCELLED, null -> return ProcessingState.Failed()
            }
        }
        return ProcessingState.Failed()
    }

    private companion object {
        const val COMPLETED = "COMPLETED"
        const val FAILED = "FAILED"
        const val CANCELLED = "CANCELLED"
        val FINISHED_WITHOUT_TEXT = setOf(FAILED, CANCELLED)
        const val RECOGNIZING_STAGE = 3
        const val POLL_INTERVAL_MILLIS = 2_000L
        /** Slightly longer than the backend's own six-minute recognition limit. */
        const val MAX_POLLS = 200
        const val MAX_NETWORK_FAILURES = 3
    }
}
