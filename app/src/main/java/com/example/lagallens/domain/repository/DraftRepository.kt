package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.Draft
import com.example.lagallens.domain.model.DraftStage

interface DraftRepository {
    suspend fun getDrafts(): List<Draft>
    suspend fun getDraft(draftId: String): Draft?
    suspend fun getLatestDraft(stage: DraftStage): Draft?
    suspend fun saveDraft(
        stage: DraftStage,
        title: String,
        content: String,
        contractId: String? = null,
        sourceDraftId: String? = null
    ): Draft
    suspend fun createCopy(sourceDraftId: String, title: String, stage: DraftStage = DraftStage.COPY): Draft?
    suspend fun updateDraft(draftId: String, title: String, content: String): Draft?
    suspend fun deleteDraft(draftId: String): Boolean
}
