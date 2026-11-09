package com.example.lagallens.data.repository

import android.content.Context
import com.example.lagallens.R
import com.example.lagallens.core.network.BackendNotConfiguredException
import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.domain.repository.DraftAiRepository
import com.example.lagallens.domain.repository.DraftEditProposal
import com.example.lagallens.domain.repository.DraftEditRequest
import com.example.lagallens.domain.repository.DraftGenerationRequest
import com.example.lagallens.domain.repository.DraftRepository
import com.example.lagallens.domain.repository.latestDraft
import java.util.UUID
import kotlinx.coroutines.delay

/** DEMO only: no AI runs. Content is fixed sample text and the delays stand in for generation time. */
internal class FakeDraftAiRepository(
    private val context: Context,
    private val drafts: DraftRepository,
    private val generationDelayMillis: Long = 1_800L,
    private val applyDelayMillis: Long = 1_400L
) : DraftAiRepository {

    override suspend fun generateDraft(request: DraftGenerationRequest): Draft {
        delay(generationDelayMillis)
        return create(DraftStage.ORIGINAL, request.contractId, request.conversationId)
    }

    override suspend fun resolveSource(sourceDraftId: String?, sourceKey: String, contractId: String?): Draft {
        val stage = DraftStage.fromLegacyKey(sourceKey) ?: DraftStage.ORIGINAL
        return sourceDraftId?.let { drafts.getDraft(it) } ?: drafts.latestDraft(stage, contractId) ?: create(stage, contractId)
    }

    override suspend fun regenerate(sourceDraftId: String?, sourceKey: String, contractId: String?): Draft {
        delay(generationDelayMillis)
        val source = resolveSource(sourceDraftId, sourceKey, contractId)
        return checkNotNull(drafts.createCopy(source.draftId, title(DraftStage.REGENERATED), DraftStage.REGENERATED, isTitleGenerated = true)) { "Regenerated draft could not be created" }
    }

    /** The sample proposal ignores the request; only a backend can read the draft and instruction. */
    override suspend fun proposeEdit(request: DraftEditRequest): DraftEditProposal {
        delay(generationDelayMillis)
        return DraftEditProposal(
            proposalId = UUID.randomUUID().toString(),
            clauseTitle = context.getString(R.string.chatbot_draft_ai_review_clause),
            originalText = context.getString(R.string.chatbot_draft_ai_review_before),
            proposedText = context.getString(R.string.chatbot_draft_section_3_approved_body),
            reason = context.getString(R.string.chatbot_draft_ai_review_reason)
        )
    }

    override suspend fun applyEdit(proposalId: String, sourceDraftId: String?, contractId: String?): Draft {
        require(proposalId.isNotBlank()) { "Unknown proposal" }
        delay(applyDelayMillis)
        val source = resolveSource(sourceDraftId, DraftStage.ORIGINAL.legacyKey, contractId)
        return drafts.saveDraft(
            stage = DraftStage.AI_PROPOSAL,
            title = title(DraftStage.AI_PROPOSAL),
            content = DraftContentFactory.applyAiProposal(context, source.content),
            contractId = source.contractId ?: contractId,
            sourceDraftId = source.draftId,
            isTitleGenerated = true
        )
    }

    override suspend fun copyVersion(sourceDraftId: String?, contractId: String?): Draft {
        delay(applyDelayMillis)
        val source = resolveSource(sourceDraftId, DraftStage.ORIGINAL.legacyKey, contractId)
        return checkNotNull(drafts.createCopy(source.draftId, title(DraftStage.COPY), DraftStage.COPY, isTitleGenerated = true)) { "Draft copy could not be created" }
    }

    private suspend fun create(stage: DraftStage, contractId: String?, conversationId: String? = null): Draft =
        drafts.saveDraft(stage, title(stage), DraftContentFactory.create(context, stage), contractId, conversationId = conversationId, isTitleGenerated = true)

    private fun title(stage: DraftStage) = context.getString(R.string.chatbot_draft_document_name, stage.legacyKey)
}

class UnconfiguredRemoteDraftAiRepository : DraftAiRepository {
    private fun unavailable(): Nothing = throw BackendNotConfiguredException("Draft AI")
    override suspend fun generateDraft(request: DraftGenerationRequest): Draft = unavailable()
    override suspend fun resolveSource(sourceDraftId: String?, sourceKey: String, contractId: String?): Draft = unavailable()
    override suspend fun regenerate(sourceDraftId: String?, sourceKey: String, contractId: String?): Draft = unavailable()
    override suspend fun proposeEdit(request: DraftEditRequest): DraftEditProposal = unavailable()
    override suspend fun applyEdit(proposalId: String, sourceDraftId: String?, contractId: String?): Draft = unavailable()
    override suspend fun copyVersion(sourceDraftId: String?, contractId: String?): Draft = unavailable()
}
