package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.EmploymentDemoDataSource
import com.example.lagallens.domain.model.EmploymentDocument
import com.example.lagallens.domain.repository.DocumentRepository

class FakeDocumentRepository(private val dataSource: EmploymentDemoDataSource) : DocumentRepository {
    override suspend fun getDocument(documentId: String): EmploymentDocument? = dataSource.getDocument(documentId)
    override suspend fun saveDocument(document: EmploymentDocument): EmploymentDocument = dataSource.saveDocument(document)
    override suspend fun updateDocumentContent(documentId: String, contractId: String?, content: String): Boolean =
        dataSource.updateDocumentContent(documentId, contractId, content)
    override suspend fun deleteDocument(documentId: String): Boolean = dataSource.deleteContractForDocument(documentId)
}
