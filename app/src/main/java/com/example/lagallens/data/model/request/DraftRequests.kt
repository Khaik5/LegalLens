package com.example.lagallens.data.model.request

data class CreateDraftRequest(val title: String, val content: String, val sourceContractId: Long? = null, val conversationId: Long? = null)

/** A stale [expectedVersion] is rejected with 409, so an edit never overwrites a newer version. */
data class UpdateDraftRequest(val expectedVersion: Int, val title: String? = null, val content: String? = null)

/** EDIT revises [draftId] at [baseVersion]; GENERATE writes a new draft from [conversationId], keeping the requests of [sourceDraftId] when it redoes that draft. */
data class DraftAiJobRequest(
    val action: String,
    val instruction: String,
    val draftId: Long? = null,
    val baseVersion: Int? = null,
    val conversationId: Long? = null,
    val contractId: Long? = null,
    val title: String? = null,
    val sourceDraftId: Long? = null
)
