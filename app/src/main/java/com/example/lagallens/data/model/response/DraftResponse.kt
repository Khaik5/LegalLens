package com.example.lagallens.data.model.response

data class DraftPageResponse(val data: List<DraftSummaryResponse>, val total: Long?, val page: Int?, val limit: Int?)

/** A draft in the list: the current version without its text. */
data class DraftSummaryResponse(
    val id: Long,
    val version: Int,
    val sourceContractId: Long?,
    val sourceDraftId: Long?,
    val conversationId: Long?,
    val title: String?,
    val createdAt: String?,
    val updatedAt: String?
)

/** A draft with the text of its current version. */
data class DraftDetailResponse(
    val id: Long,
    val version: Int,
    val sourceContractId: Long?,
    val sourceDraftId: Long?,
    val conversationId: Long?,
    val title: String?,
    val content: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class DraftVersionListResponse(val data: List<DraftVersionSummaryResponse>)

data class DraftVersionSummaryResponse(val version: Int, val title: String?, val createdAt: String?)

data class DraftVersionResponse(val version: Int, val title: String?, val content: String?, val createdAt: String?)

/** Returned by create, update, copy and apply. */
data class DraftSavedResponse(val id: Long, val version: Int)

/** [status] is WAITING_PROVIDER, PROCESSING, COMPLETED, APPLIED, FAILED or CANCELLED. */
data class DraftAiJobCreatedResponse(val jobId: String, val draftId: Long, val status: String)

data class DraftAiJobResponse(
    val jobId: String,
    val draftId: Long,
    val baseVersion: Int,
    val action: String?,
    val status: String,
    val suggestion: String?,
    val errorCode: String?,
    val appliedVersion: Int?,
    val changes: List<DraftAiChangeResponse>?
)

data class DraftAiChangeResponse(val id: String?, val clauseTitle: String?, val originalText: String?, val proposedText: String?, val reason: String?)
