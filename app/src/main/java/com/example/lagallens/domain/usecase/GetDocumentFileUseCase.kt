package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.EmploymentDocument
import com.example.lagallens.domain.repository.DocumentRepository

class GetDocumentFileUseCase(private val documents: DocumentRepository) {
    suspend fun findForContract(contractId: String, sessionKey: String): EmploymentDocument? =
        documents.forSession(sessionKey).listDocuments(contractId).maxByOrNull { it.updatedAtMillis }

    suspend operator fun invoke(documentId: String, sessionKey: String, forDownload: Boolean = false): String =
        documents.forSession(sessionKey).cacheDocumentFile(documentId, forDownload)

    fun release(fileUri: String) = documents.releaseDocumentFile(fileUri)
}
