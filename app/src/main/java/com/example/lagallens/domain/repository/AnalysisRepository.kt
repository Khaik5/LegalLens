package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.analysis.AnalysisOverview

interface AnalysisRepository {
    /** False when no analysis service can run a new analysis, so none should be started. */
    val isAvailable: Boolean get() = true

    /** Idempotent by operationId, so a retry after process death cannot create a second result. */
    fun process(operationId: String, contractId: String, documentId: String, sessionKey: String): kotlinx.coroutines.flow.Flow<com.example.lagallens.domain.model.processing.AnalysisJobState>
    suspend fun getOverview(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity): AnalysisOverview
    suspend fun getDetail(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, language: String): com.example.lagallens.domain.model.analysis.AnalysisDetail
}
