package com.example.lagallens.presentation.feature.ocr.result.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.data.repository.FakeOcrRepository
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractComparison
import com.example.lagallens.domain.model.EmploymentDocument
import com.example.lagallens.domain.usecase.GetOcrResultUseCase
import com.example.lagallens.domain.repository.ContractRepository
import com.example.lagallens.domain.repository.DocumentRepository
import com.example.lagallens.presentation.feature.ocr.result.contract.OcrResultUiEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class OcrResultViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun documentUpdateReplacesTheDisplayedOcrText() {
        val viewModel = viewModel()
        val editedText = "Nội dung OCR đã được người dùng chỉnh sửa"
        viewModel.onEvent(OcrResultUiEvent.DocumentUpdated(editedText))
        assertEquals(editedText, viewModel.uiState.value.documentText)
    }

    @Test
    fun reviewConfirmationSavesDocumentAndCreatesLinkedContract() {
        val contracts = MemoryContractRepository()
        val documents = MemoryDocumentRepository()
        val viewModel = viewModel(contracts, documents)
        val documentId = "document-1"
        val editedText = "HỢP ĐỒNG LAO ĐỘNG DEMO\nĐIỀU 1. Công việc\n[Chức danh]"
        viewModel.onEvent(OcrResultUiEvent.ScreenOpened("Hợp đồng lao động DEMO", "PDF", documentId, initialContent = editedText))
        viewModel.onEvent(OcrResultUiEvent.ReviewConfirmed(editedText))

        val savedDocument = kotlinx.coroutines.runBlocking { documents.getDocument(documentId) }
        val savedContract = contracts.records.values.singleOrNull()
        assertNotNull(savedDocument)
        assertNotNull(savedContract)
        assertEquals(editedText, savedDocument?.content)
        assertEquals(savedContract?.id, savedDocument?.contractId)
        assertEquals(documentId, savedContract?.documentId)
        assertEquals(editedText, savedContract?.content)
        assertEquals(true, viewModel.uiState.value.isReviewConfirmed)
    }

    @Test
    fun reviewConfirmationIsKeptForTheCurrentSession() {
        val viewModel = viewModel()
        viewModel.onEvent(OcrResultUiEvent.ScreenOpened("Hợp đồng DEMO", "PDF", "document-session", initialContent = "ĐIỀU 1. Nội dung"))
        viewModel.onEvent(OcrResultUiEvent.ReviewConfirmed("ĐIỀU 1. Nội dung", "document-session", null))
        assertEquals(true, viewModel.uiState.value.isReviewConfirmed)
    }

    private fun viewModel(
        contracts: MemoryContractRepository = MemoryContractRepository(),
        documents: MemoryDocumentRepository = MemoryDocumentRepository()
    ) = OcrResultViewModel(GetOcrResultUseCase(FakeOcrRepository()), documents, contracts)

    private class MemoryDocumentRepository : DocumentRepository {
        private val records = mutableMapOf<String, EmploymentDocument>()
        override suspend fun getDocument(documentId: String) = records[documentId]
        override suspend fun saveDocument(document: EmploymentDocument): EmploymentDocument = document.also { records[it.id] = it }
        override suspend fun updateDocumentContent(documentId: String, contractId: String?, content: String): Boolean {
            val record = records[documentId] ?: return false
            records[documentId] = record.copy(contractId = contractId ?: record.contractId, content = content)
            return true
        }
        override suspend fun deleteDocument(documentId: String) = records.remove(documentId) != null
    }

    private class MemoryContractRepository : ContractRepository {
        val records = mutableMapOf<String, EmploymentContract>()
        private val observable = MutableStateFlow<List<EmploymentContract>>(emptyList())
        override fun observeContracts(): Flow<List<EmploymentContract>> = observable
        override suspend fun getContract(contractId: String) = records[contractId]
        override suspend fun createContract(contract: EmploymentContract): EmploymentContract = contract.also { records[it.id] = it; refresh() }
        override suspend fun updateContract(contract: EmploymentContract): Boolean {
            if (contract.id !in records) return false
            records[contract.id] = contract
            refresh()
            return true
        }
        override suspend fun renameContract(contractId: String, title: String): Boolean {
            val record = records[contractId] ?: return false
            return updateContract(record.copy(title = title))
        }
        override suspend fun deleteContract(contractId: String): Boolean = (records.remove(contractId) != null).also { refresh() }
        override suspend fun compareContracts(firstId: String, secondId: String): EmploymentContractComparison? = null
        private fun refresh() { observable.value = records.values.toList() }
    }
}
