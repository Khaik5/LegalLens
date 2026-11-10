package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.EmploymentDocument

interface DocumentRepository {
    fun forSession(sessionKey: String): DocumentRepository = throw IllegalStateException("Session-bound documents unavailable")
    suspend fun getDocument(documentId: String): EmploymentDocument?
    suspend fun listDocuments(contractId: String): List<EmploymentDocument> = emptyList()
    suspend fun cacheDocumentFile(documentId: String, forDownload: Boolean): String =
        throw UnsupportedOperationException("Remote document file unavailable")
    /** Removes a file previously returned by [cacheDocumentFile]; other locations are ignored. */
    fun releaseDocumentFile(fileUri: String) = Unit
    suspend fun saveDocument(document: EmploymentDocument): EmploymentDocument
    suspend fun updateDocumentContent(documentId: String, contractId: String?, content: String): Boolean
    suspend fun deleteDocument(documentId: String): Boolean
}
