package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.EmploymentDocument

interface DocumentRepository {
    fun forSession(sessionKey: String): DocumentRepository = throw IllegalStateException("Session-bound documents unavailable")
    suspend fun getDocument(documentId: String): EmploymentDocument?
    suspend fun saveDocument(document: EmploymentDocument): EmploymentDocument
    suspend fun updateDocumentContent(documentId: String, contractId: String?, content: String): Boolean
    suspend fun deleteDocument(documentId: String): Boolean
}
