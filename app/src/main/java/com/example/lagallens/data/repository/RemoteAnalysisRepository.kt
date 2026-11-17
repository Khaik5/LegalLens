package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.remote.AnalysisRemoteDataSource
import com.example.lagallens.data.datasource.remote.OcrRemoteDataSource
import com.example.lagallens.data.mapper.AnalysisObligationLabels
import com.example.lagallens.data.mapper.AnalysisResultMapper
import com.example.lagallens.data.model.request.AnalysisJobRequest
import com.example.lagallens.domain.model.analysis.AnalysisDetail
import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import com.example.lagallens.domain.model.analysis.AnalysisOverview
import com.example.lagallens.domain.model.processing.AnalysisJobState
import com.example.lagallens.domain.repository.AnalysisRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Runs a contract analysis on the backend. The job needs an owner-approved OCR revision, so the
 * revision the user just reviewed is confirmed first; starting the analysis is the user's approval.
 */
class RemoteAnalysisRepository(
    private val tokens: AuthSessionStore,
    private val analysis: AnalysisRemoteDataSource,
    private val ocr: OcrRemoteDataSource,
    private val obligationLabels: () -> AnalysisObligationLabels,
    private val pollIntervalMillis: Long = POLL_INTERVAL_MILLIS,
    private val maxPolls: Int = MAX_POLLS
) : AnalysisRepository {
    override fun process(operationId: String, contractId: String, documentId: String, sessionKey: String): Flow<AnalysisJobState> = flow {
        val scope = tokens.currentSessionScope()?.takeIf { it.key == sessionKey }
        val contract = contractId.toLongOrNull()
        val document = documentId.toLongOrNull()
        if (scope == null || contract == null || document == null) {
            emit(AnalysisJobState.Failed)
            return@flow
        }
        try {
            emit(AnalysisJobState.Running(FIRST_STAGE))
            val revisionId = approvedRevisionId(scope, documentId)
            if (revisionId == null) {
                emit(AnalysisJobState.Failed)
                return@flow
            }
            // The operation ID is the idempotency key, so a retry after process death joins the same job.
            val jobId = analysis.create(scope, operationId, AnalysisJobRequest(contract, document, revisionId)).jobId
            awaitJob(scope, jobId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: HttpException) {
            error.response()?.errorBody()?.close()
            emit(AnalysisJobState.Failed)
        } catch (_: IOException) {
            emit(AnalysisJobState.Failed)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getOverview(identity: AnalysisIdentity): AnalysisOverview =
        identity.overview ?: AnalysisResultMapper.toOverview(loadResult(identity))

    override suspend fun getDetail(identity: AnalysisIdentity, language: String): AnalysisDetail =
        AnalysisResultMapper.toDetail(loadResult(identity), obligationLabels())

    private suspend fun loadResult(identity: AnalysisIdentity) = withContext(Dispatchers.IO) {
        val scope = AuthSessionScope(checkNotNull(tokens.currentSessionScope()) { "No active session" }.key)
        analysis.result(scope, identity.resultId)
    }

    /** Null when the document has no finished OCR text to analyse. */
    private suspend fun approvedRevisionId(scope: AuthSessionScope, documentId: String): String? {
        val latest = ocr.latest(scope, documentId) ?: return null
        val revision = latest.revision ?: return null
        if (latest.status != "COMPLETED") return null
        if (latest.reviewStatus == APPROVED && latest.revisionId != null) return latest.revisionId
        return ocr.confirm(scope, documentId, revision).revisionId
    }

    /** Emits a stage whenever the reported progress moves the timeline forward, then the final state. */
    private suspend fun FlowCollector<AnalysisJobState>.awaitJob(scope: AuthSessionScope, jobId: String) {
        var networkFailures = 0
        var stage = FIRST_STAGE
        repeat(maxPolls) {
            delay(pollIntervalMillis)
            val job = try {
                analysis.job(scope, jobId).also { networkFailures = 0 }
            } catch (error: IOException) {
                if (++networkFailures > MAX_NETWORK_FAILURES) throw error
                return@repeat
            }
            when (job.status) {
                "COMPLETED" -> return emit(AnalysisJobState.Completed(jobId))
                "FAILED", "CANCELLED" -> return emit(AnalysisJobState.Failed)
            }
            // The timeline has stages FIRST_STAGE..LAST_STAGE; progress 0..100 moves through them.
            val next = (FIRST_STAGE + (job.progress ?: 0).coerceIn(0, 100) * (LAST_STAGE - FIRST_STAGE) / 100)
            if (next > stage) {
                stage = next
                emit(AnalysisJobState.Running(stage))
            }
        }
        emit(AnalysisJobState.Failed)
    }

    private companion object {
        const val APPROVED = "APPROVED"
        const val FIRST_STAGE = 3
        const val LAST_STAGE = 6
        const val POLL_INTERVAL_MILLIS = 2_000L
        /** Slightly longer than the backend's usual 90 seconds for an analysis. */
        const val MAX_POLLS = 150
        const val MAX_NETWORK_FAILURES = 3
    }
}
