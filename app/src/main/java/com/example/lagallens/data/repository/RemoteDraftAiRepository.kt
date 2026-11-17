package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.remote.DraftRemoteDataSource
import com.example.lagallens.data.model.request.DraftAiJobRequest
import com.example.lagallens.data.model.response.DraftAiJobResponse
import com.example.lagallens.domain.model.Draft
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

/** The job could not produce text: the provider failed, was cancelled, or did not answer in time. */
class DraftAiJobFailedException(val errorCode: String?) : IllegalStateException("Draft AI job failed")

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
        val (draftId, _) = requireNotNull(request.draftId?.let(RemoteDraftId::parse)) { "Unknown draft" }
        // The suggestion is made on the newest backend version of this draft.
        val baseVersion = remote.get(scope, draftId).version
        val instruction = listOf(request.scope, request.instruction).filter { it.isNotBlank() }.joinToString("\n")
        val created = remote.createJob(scope, UUID.randomUUID().toString(), DraftAiJobRequest("EDIT", instruction, draftId = draftId, baseVersion = baseVersion))
        val job = awaitJob(scope, created.jobId)
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
        val saved = remote.apply(scope, proposalId)
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
            val created = remote.createJob(
                scope, UUID.randomUUID().toString(),
                DraftAiJobRequest("GENERATE", instruction, conversationId = conversation, contractId = contractId?.toLongOrNull())
            )
            awaitJob(scope, created.jobId)
            // The generated text is written as the next version of the new, still empty draft.
            val saved = remote.apply(scope, created.jobId)
            marks.put(userId, saved.id, saved.version, DraftVersionMark(stage, isSaved = false))
            drafts.loadVersion(scope, userId, saved.id, saved.version)
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
                "FAILED", "CANCELLED" -> throw DraftAiJobFailedException(job.errorCode)
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
        throw DraftAiJobFailedException("TIMEOUT")
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
