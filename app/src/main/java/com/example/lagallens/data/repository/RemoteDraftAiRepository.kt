package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.remote.DraftRemoteDataSource
import com.example.lagallens.data.model.request.DraftAiJobRequest
import com.example.lagallens.data.model.response.DraftAiJobCreatedResponse
import com.example.lagallens.data.model.response.DraftAiJobResponse
import retrofit2.HttpException
import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftAiFailedException
import com.example.lagallens.domain.model.DraftAiFailureReason
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.repository.DraftAiRepository
import com.example.lagallens.domain.repository.DraftEditProposal
import com.example.lagallens.domain.repository.DraftEditRequest
import com.example.lagallens.domain.repository.DraftGenerationRequest
import com.example.lagallens.domain.repository.latestDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Draft generation and edit suggestions from the backend. A generated text or a suggestion only becomes a
 * version when it is applied, so the user's draft is never changed by a request alone.
 */
class RemoteDraftAiRepository(
    private val tokens: AuthSessionStore,
    private val remote: DraftRemoteDataSource,
    private val drafts: RemoteDraftRepository,
    private val marks: DraftVersionMarks,
    /** Instruction that asks the backend to write the contract again from the same conversation. */
    private val regenerateInstruction: () -> String,
    private val pollIntervalMillis: Long = POLL_INTERVAL_MILLIS,
    private val maxPolls: Int = MAX_POLLS
) : DraftAiRepository {
    override suspend fun generateDraft(request: DraftGenerationRequest): Draft = generate(
        conversationId = request.conversationId, contractId = request.contractId,
        instruction = request.instructions.joinToString("\n"), stage = DraftStage.ORIGINAL
    )

    override suspend fun resolveSource(sourceDraftId: String?, sourceKey: String, contractId: String?): Draft {
        val stage = DraftStage.fromLegacyKey(sourceKey) ?: DraftStage.ORIGINAL
        return sourceDraftId?.let { drafts.getDraft(it) } ?: drafts.latestDraft(stage, contractId)
            ?: throw IllegalStateException("No draft to work from")
    }

    /** A new backend draft written from the same conversation; the source stays as it is. */
    override suspend fun regenerate(sourceDraftId: String?, sourceKey: String, contractId: String?): Draft {
        val source = resolveSource(sourceDraftId, sourceKey, contractId)
        return generate(source.conversationId, source.contractId ?: contractId, regenerateInstruction(), DraftStage.REGENERATED)
    }

    override suspend fun proposeEdit(request: DraftEditRequest): DraftEditProposal = inSession { scope, _ ->
        val (draftId, viewedVersion) = requireNotNull(request.draftId?.let(RemoteDraftId::parse)) { "Unknown draft" }
        // The suggestion is made on the version the user is looking at. If a newer one exists the backend answers
        // 409 instead of silently editing a different text.
        val baseVersion = viewedVersion
        val instruction = listOf(request.scope, request.instruction).filter { it.isNotBlank() }.joinToString("\n")
        val jobRequest = DraftAiJobRequest("EDIT", instruction, draftId = draftId, baseVersion = baseVersion)
        val created = startJob(scope, jobRequest)
        val job = awaitJob(scope, created.jobId)
        finishRequest(scope, jobRequest)
        val change = job.changes.orEmpty().firstOrNull()
        DraftEditProposal(
            proposalId = job.jobId,
            clauseTitle = change?.clauseTitle.orEmpty(),
            originalText = change?.originalText.orEmpty(),
            proposedText = change?.proposedText ?: job.suggestion.orEmpty(),
            reason = change?.reason.orEmpty()
        )
    }

    override suspend fun applyEdit(proposalId: String, sourceDraftId: String?, contractId: String?): Draft = inSession { scope, userId ->
        val saved = outOfDateOnConflict { remote.apply(scope, proposalId) }
        marks.put(userId, saved.id, saved.version, DraftVersionMark(DraftStage.AI_PROPOSAL, isSaved = false))
        drafts.loadVersion(scope, userId, saved.id, saved.version)
    }

    override suspend fun copyVersion(sourceDraftId: String?, contractId: String?): Draft {
        val source = resolveSource(sourceDraftId, DraftStage.ORIGINAL.legacyKey, contractId)
        return checkNotNull(drafts.createCopy(source.draftId, "", DraftStage.COPY)) { "Draft copy could not be created" }
    }

    private suspend fun generate(conversationId: String?, contractId: String?, instruction: String, stage: DraftStage): Draft =
        inSession { scope, userId ->
            val conversation = requireNotNull(conversationId?.toLongOrNull()) { "A conversation is required" }
            val jobRequest = DraftAiJobRequest("GENERATE", instruction, conversationId = conversation, contractId = contractId?.toLongOrNull())
            val created = startJob(scope, jobRequest)
            awaitJob(scope, created.jobId)
            // The generated text is written as the next version of the new, still empty draft.
            val saved = remote.apply(scope, created.jobId)
            finishRequest(scope, jobRequest)
            marks.put(userId, saved.id, saved.version, DraftVersionMark(stage, isSaved = false))
            drafts.loadVersion(scope, userId, saved.id, saved.version)
        }

    /**
     * Idempotency keys of requests that did not finish, by session and request. Asking again for the same thing
     * reuses the key, and the backend answers with the job it already has: a lost response or a failed job does
     * not start a second one (for a new draft, a second empty draft). A finished request forgets its key, so
     * asking the same thing later is a new request, as regenerating a draft is.
     */
    private val pendingKeys = java.util.concurrent.ConcurrentHashMap<String, String>()

    private fun fingerprint(scope: AuthSessionScope, request: DraftAiJobRequest) = "${scope.key}|$request"

    private suspend fun startJob(scope: AuthSessionScope, request: DraftAiJobRequest): DraftAiJobCreatedResponse {
        val fingerprint = fingerprint(scope, request)
        val key = pendingKeys.getOrPut(fingerprint) { UUID.randomUUID().toString() }
        val created = if (request.action == "GENERATE") remote.createJob(scope, key, request) else outOfDateOnConflict { remote.createJob(scope, key, request) }
        return when (created.status) {
            // A cancelled job can never run again, so the same key would only return the dead job.
            "CANCELLED" -> {
                val fresh = UUID.randomUUID().toString().also { pendingKeys[fingerprint] = it }
                if (request.action == "GENERATE") remote.createJob(scope, fresh, request) else outOfDateOnConflict { remote.createJob(scope, fresh, request) }
            }
            "FAILED" -> {
                try {
                    remote.retry(scope, created.jobId)
                } catch (error: HttpException) {
                    // Not failed any more (another call already restarted it): follow the job as it is.
                    if (error.code() != 409) throw error
                    error.response()?.errorBody()?.close()
                }
                created
            }
            else -> created
        }
    }

    private fun finishRequest(scope: AuthSessionScope, request: DraftAiJobRequest) {
        pendingKeys.remove(fingerprint(scope, request))
    }

    /** Waits for COMPLETED; short network drops are tolerated because the job keeps running on the backend. */
    private suspend fun awaitJob(scope: AuthSessionScope, jobId: String): DraftAiJobResponse {
        var networkFailures = 0
        repeat(maxPolls) {
            val job = try {
                remote.job(scope, jobId).also { networkFailures = 0 }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                if (++networkFailures > MAX_NETWORK_FAILURES) throw error
                delay(pollIntervalMillis)
                return@repeat
            }
            when (job.status) {
                "COMPLETED", "APPLIED" -> return job
                "FAILED", "CANCELLED" -> throw DraftAiFailedException(failureReason(job.errorCode))
            }
            delay(pollIntervalMillis)
        }
        // Stop a job nobody is waiting for any more; a failed cancel changes nothing for the user.
        try {
            remote.cancel(scope, jobId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The job expires on the backend by itself.
        }
        throw DraftAiFailedException(DraftAiFailureReason.TIMEOUT)
    }

    /** The backend answers 409 when the draft is no longer at the version the request was made for. */
    private suspend fun <T> outOfDateOnConflict(call: suspend () -> T): T = try {
        call()
    } catch (error: HttpException) {
        if (error.code() != 409) throw error
        error.response()?.errorBody()?.close()
        throw DraftAiFailedException(DraftAiFailureReason.OUT_OF_DATE)
    }

    private fun failureReason(errorCode: String?) = when (errorCode) {
        "AI_TIMEOUT", "TIMEOUT" -> DraftAiFailureReason.TIMEOUT
        "AI_OVERLOADED" -> DraftAiFailureReason.OVERLOADED
        "PII_REDACTION_FAILED", "PII_RESTORE_FAILED" -> DraftAiFailureReason.PRIVACY_PROTECTION
        "DRAFT_SOURCE_TEXT_UNAVAILABLE" -> DraftAiFailureReason.SOURCE_UNAVAILABLE
        else -> DraftAiFailureReason.OTHER
    }

    private suspend fun <T> inSession(action: suspend (AuthSessionScope, String) -> T): T = withContext(Dispatchers.IO) {
        val scope = checkNotNull(tokens.currentSessionScope()) { "No active session" }
        val userId = checkNotNull(tokens.userId()) { "No active session" }
        val result = action(scope, userId)
        check(tokens.currentSessionScope()?.key == scope.key) { "Session changed" }
        result
    }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 2_500L
        /** Slightly longer than the backend's usual 30 seconds for a draft job. */
        const val MAX_POLLS = 120
        const val MAX_NETWORK_FAILURES = 3
    }
}
