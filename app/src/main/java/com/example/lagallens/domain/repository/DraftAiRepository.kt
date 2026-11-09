package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.Draft

data class DraftGenerationRequest(
    val conversationId: String?,
    val contractId: String?,
    val instructions: List<String>
)

data class DraftEditRequest(
    val conversationId: String?,
    val contractId: String?,
    val draftId: String?,
    val scope: String,
    val instruction: String
)

/** A suggested change. Nothing is written until [proposalId] is applied. */
data class DraftEditProposal(
    val proposalId: String,
    val clauseTitle: String,
    val originalText: String,
    val proposedText: String,
    val reason: String
)

/** Draft operations that need generated content; every result is stored before it is returned. */
interface DraftAiRepository {
    suspend fun generateDraft(request: DraftGenerationRequest): Draft
    /** Returns the draft to work from, creating the first generated version when none exists yet. */
    suspend fun resolveSource(sourceDraftId: String?, sourceKey: String, contractId: String?): Draft
    suspend fun regenerate(sourceDraftId: String?, sourceKey: String, contractId: String?): Draft
    suspend fun proposeEdit(request: DraftEditRequest): DraftEditProposal
    suspend fun applyEdit(proposalId: String, sourceDraftId: String?, contractId: String?): Draft
    suspend fun copyVersion(sourceDraftId: String?, contractId: String?): Draft
}
