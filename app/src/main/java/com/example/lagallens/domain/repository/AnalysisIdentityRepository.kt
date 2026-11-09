package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.analysis.AnalysisIdentity

class AnalysisContentUnavailableException : IllegalStateException("Analysis content unavailable")

interface AnalysisIdentityRepository {
    fun sessionKey(): String?
    suspend fun create(operationId: String, contractId: String, documentId: String, sessionKey: String): AnalysisIdentity
    suspend fun get(resultId: String, sessionKey: String): AnalysisIdentity?
    suspend fun latest(contractId: String, sessionKey: String): AnalysisIdentity?
}
