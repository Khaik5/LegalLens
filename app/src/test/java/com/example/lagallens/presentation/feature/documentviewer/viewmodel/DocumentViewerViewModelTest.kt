package com.example.lagallens.presentation.feature.documentviewer.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.core.network.DocumentFiles
import com.example.lagallens.domain.model.EmploymentDocument
import com.example.lagallens.domain.repository.DocumentRepository
import com.example.lagallens.domain.usecase.GetDocumentFileUseCase
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerStatus
import com.example.lagallens.presentation.feature.documentviewer.contract.DocumentViewerUiEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class DocumentViewerViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    private val repository = DocumentFixture()
    private val viewModel = DocumentViewerViewModel(GetDocumentFileUseCase(repository))
    private val open = DocumentViewerUiEvent.OpenRemote("3", "session")

    @Test fun pdfPreviewIsCachedForTheLatestDocument() = runTest {
        repository.documents = listOf(document("1", DocumentFiles.PDF, 10), document("2", DocumentFiles.PDF, 20))

        viewModel.onEvent(open)

        assertEquals("2", viewModel.uiState.value.remoteDocument?.id)
        assertEquals("file:///cache/2-1", viewModel.uiState.value.previewUri)
    }

    @Test fun contractWithoutDocumentShowsError() = runTest {
        viewModel.onEvent(open)

        assertEquals(DocumentViewerStatus.ERROR, viewModel.uiState.value.status)
        assertNull(viewModel.uiState.value.previewUri)
    }

    @Test fun wordDocumentIsNotPreviewed() = runTest {
        repository.documents = listOf(document("1", DocumentFiles.DOCX, 10))

        viewModel.onEvent(open)

        assertEquals(DocumentViewerStatus.ERROR, viewModel.uiState.value.status)
        assertEquals(0, repository.cacheCalls)
    }

    @Test fun retryAfterFailureLoadsAgainAndReleasesOldPreview() = runTest {
        repository.documents = listOf(document("1", DocumentFiles.JPEG, 10))
        repository.failure = IOException("offline")
        viewModel.onEvent(open)
        assertEquals(DocumentViewerStatus.ERROR, viewModel.uiState.value.status)

        repository.failure = null
        viewModel.onEvent(DocumentViewerUiEvent.RetryClicked)
        assertEquals("file:///cache/1-1", viewModel.uiState.value.previewUri)

        viewModel.onEvent(DocumentViewerUiEvent.RetryClicked)
        assertEquals("file:///cache/1-2", viewModel.uiState.value.previewUri)
        assertEquals(listOf("file:///cache/1-1"), repository.released)
    }

    private fun document(id: String, mimeType: String, updatedAt: Long) =
        EmploymentDocument(id, "3", "$id.file", null, mimeType, "", updatedAt)

    private class DocumentFixture : DocumentRepository {
        var documents = emptyList<EmploymentDocument>()
        var failure: Exception? = null
        var cacheCalls = 0
        val released = mutableListOf<String>()

        override fun forSession(sessionKey: String): DocumentRepository = this
        override suspend fun listDocuments(contractId: String) = documents
        override suspend fun cacheDocumentFile(documentId: String, forDownload: Boolean): String {
            failure?.let { throw it }
            cacheCalls++
            return "file:///cache/$documentId-$cacheCalls"
        }
        override fun releaseDocumentFile(fileUri: String) { released += fileUri }
        override suspend fun getDocument(documentId: String) = documents.firstOrNull { it.id == documentId }
        override suspend fun saveDocument(document: EmploymentDocument) = document
        override suspend fun updateDocumentContent(documentId: String, contractId: String?, content: String) = false
        override suspend fun deleteDocument(documentId: String) = false
    }
}
