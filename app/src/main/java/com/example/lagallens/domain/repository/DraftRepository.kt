package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftStage

interface DraftRepository {
    fun forSession(sessionKey: String?): DraftRepository = this
    suspend fun getDrafts(): List<Draft>
    suspend fun getDraft(draftId: String): Draft?
    suspend fun getLatestDraft(stage: DraftStage): Draft?
    suspend fun saveDraft(
        stage: DraftStage,
        title: String,
        content: String,
        contractId: String? = null,
        sourceDraftId: String? = null,
        conversationId: String? = null,
        isSaved: Boolean = false,
        isTitleGenerated: Boolean = false
    ): Draft
    suspend fun createCopy(sourceDraftId: String, title: String, stage: DraftStage = DraftStage.COPY, isTitleGenerated: Boolean = false): Draft?
    suspend fun updateDraft(draftId: String, title: String, content: String): Draft?
    suspend fun deleteDraft(draftId: String): Boolean
}

/** Versions of one contract never mix with another contract's drafts. */
suspend fun DraftRepository.latestDraft(stage: DraftStage, contractId: String?): Draft? =
    if (contractId == null) getLatestDraft(stage)
    else getDrafts().filter { it.stage == stage && it.contractId == contractId }
        .maxWithOrNull(compareBy<Draft> { it.versionNo }.thenBy { it.updatedAt })
