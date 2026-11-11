package com.example.lagallens.presentation.feature.contracts.detail.export.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.model.EmploymentDocument
import com.example.lagallens.domain.repository.DocumentRepository
import com.example.lagallens.domain.usecase.GetDocumentFileUseCase
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportStatus
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportUiEffect
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportUiEvent
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ContractExportViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    private val documents = DocumentsFixture()
    private fun viewModel(contractId: String = "7", session: String = "session") =
        ContractExportViewModel(GetDocumentFileUseCase(documents), contractId, session)

    @Test fun savingDownloadsTheNewestOriginalDocument() = runTest {
        val model = viewModel()

        model.onEvent(ContractExportUiEvent.ExportClicked)

        assertEquals(
            ContractExportUiEffect.SaveFile("file://cache/12", "Hợp đồng lao động.pdf", "application/pdf"),
            model.uiEffect.first()
        )
        assertEquals(listOf("12" to true), documents.downloads)
        assertEquals(ContractExportStatus.OPTIONS, model.uiState.value.status)
    }

    @Test fun sharingHandsOverTheOriginalFileType() = runTest {
        documents.items = listOf(document("20", "Bản scan.jpg", "image/jpeg", updatedAt = 5))
        val model = viewModel()

        model.onEvent(ContractExportUiEvent.ShareClicked)

        assertEquals(ContractExportUiEffect.ShareFile("file://cache/20", "Bản scan.jpg", "image/jpeg"), model.uiEffect.first())
    }

    @Test fun aContractWithoutDocumentsShowsTheError() = runTest {
        documents.items = emptyList()
        val model = viewModel()

        model.onEvent(ContractExportUiEvent.ExportClicked)

        assertEquals(ContractExportStatus.ERROR, model.uiState.value.status)
        assertEquals(emptyList<Pair<String, Boolean>>(), documents.downloads)
    }

    @Test fun retryRepeatsTheFailedAction() = runTest {
        documents.failure = IOException("offline")
        val model = viewModel()
        model.onEvent(ContractExportUiEvent.ShareClicked)
        assertEquals(ContractExportStatus.ERROR, model.uiState.value.status)

        documents.failure = null
        model.onEvent(ContractExportUiEvent.RetryClicked)

        assertEquals(ContractExportUiEffect.ShareFile("file://cache/12", "Hợp đồng lao động.pdf", "application/pdf"), model.uiEffect.first())
    }

    @Test fun aSecondTapWhileDownloadingIsIgnored() = runTest {
        val gate = CompletableDeferred<Unit>()
        documents.beforeDownload = { gate.await() }
        val model = viewModel()

        model.onEvent(ContractExportUiEvent.ExportClicked)
        model.onEvent(ContractExportUiEvent.ShareClicked)
        assertEquals(ContractExportStatus.LOADING, model.uiState.value.status)
        gate.complete(Unit)

        assertEquals(ContractExportUiEffect.SaveFile("file://cache/12", "Hợp đồng lao động.pdf", "application/pdf"), model.uiEffect.first())
        assertEquals(1, documents.downloads.size)
    }

    @Test fun aMissingSessionNeverDownloads() = runTest {
        val model = viewModel(session = "")

        model.onEvent(ContractExportUiEvent.ExportClicked)

        assertEquals(ContractExportStatus.ERROR, model.uiState.value.status)
        assertEquals(emptyList<Pair<String, Boolean>>(), documents.downloads)
    }

    @Test fun releasingAnUnusedDownloadDeletesIt() = runTest {
        val model = viewModel()

        model.onEvent(ContractExportUiEvent.DownloadReleased("file://cache/12"))

        assertEquals(listOf("file://cache/12"), documents.released)
    }

    private fun document(id: String, name: String, mimeType: String, updatedAt: Long) =
        EmploymentDocument(id, "7", name, null, mimeType, "", updatedAt)

    private inner class DocumentsFixture : DocumentRepository {
        var items = listOf(
            document("11", "Bản cũ.pdf", "application/pdf", updatedAt = 1),
            document("12", "Hợp đồng lao động.pdf", "application/pdf", updatedAt = 2)
        )
        var failure: Exception? = null
        var beforeDownload: suspend () -> Unit = {}
        val downloads = mutableListOf<Pair<String, Boolean>>()
        val released = mutableListOf<String>()

        override fun forSession(sessionKey: String): DocumentRepository {
            assertEquals("session", sessionKey)
            return this
        }

        override suspend fun listDocuments(contractId: String): List<EmploymentDocument> {
            assertEquals("7", contractId)
            return items
        }

        override suspend fun cacheDocumentFile(documentId: String, forDownload: Boolean): String {
            beforeDownload()
            failure?.let { throw it }
            downloads.add(documentId to forDownload)
            return "file://cache/$documentId"
        }

        override fun releaseDocumentFile(fileUri: String) {
            released.add(fileUri)
        }

        override suspend fun getDocument(documentId: String) = error("Unexpected")
        override suspend fun saveDocument(document: EmploymentDocument) = error("Unexpected")
        override suspend fun updateDocumentContent(documentId: String, contractId: String?, content: String) = error("Unexpected")
        override suspend fun deleteDocument(documentId: String) = error("Unexpected")
    }
}
