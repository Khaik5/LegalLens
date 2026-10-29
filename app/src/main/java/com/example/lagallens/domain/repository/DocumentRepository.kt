package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.EmploymentDocument

interface DocumentRepository {
    suspend fun getDocument(documentId: String): EmploymentDocument?
    suspend fun saveDocument(document: EmploymentDocument): EmploymentDocument
    suspend fun updateDocumentContent(documentId: String, contractId: String?, content: String): Boolean
    suspend fun deleteDocument(documentId: String): Boolean
}
