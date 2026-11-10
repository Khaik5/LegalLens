package com.example.lagallens.presentation.feature.upload.progress.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.model.processing.DocumentUploadRequest
import com.example.lagallens.domain.model.processing.ProcessingState
import com.example.lagallens.domain.model.processing.UploadFailure
import com.example.lagallens.domain.repository.DocumentUploadRepository
import kotlinx.coroutines.flow.flowOf
import com.example.lagallens.presentation.feature.upload.progress.contract.UploadProgressEffect
import com.example.lagallens.presentation.feature.upload.progress.contract.UploadProgressEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UploadProgressViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    @Test fun reopeningSameScreenDoesNotRestartProcessingOrChangeDocument() = runTest {
        val vm = UploadProgressViewModel(com.example.lagallens.data.repository.FakeDocumentUploadRepository())
        val effects = mutableListOf<UploadProgressEffect>()
        backgroundScope.launch { vm.effects.collect { effects.add(it) } }
        runCurrent()
        val event = UploadProgressEvent.ScreenOpened("file", "pdf", "content://fixture", "doc-a")
        vm.onEvent(event)
        advanceTimeBy(400); runCurrent()
        vm.onEvent(event)
        advanceTimeBy(601); runCurrent()
        assertEquals(listOf(UploadProgressEffect.NavigateToOcrProcessing("file", "pdf", "content://fixture", "doc-a")), effects)
    }

    @Test fun reopeningAfterTheUploadFinishedDoesNotUploadAgain() = runTest {
        val vm = UploadProgressViewModel(com.example.lagallens.data.repository.FakeDocumentUploadRepository())
        val effects = mutableListOf<UploadProgressEffect>()
        backgroundScope.launch { vm.effects.collect { effects.add(it) } }
        runCurrent()
        val event = UploadProgressEvent.ScreenOpened("file", "pdf", "content://fixture", "doc-a")
        vm.onEvent(event)
        advanceTimeBy(2000); runCurrent()
        vm.onEvent(event)
        advanceTimeBy(2000); runCurrent()
        assertEquals(1, effects.count { it is UploadProgressEffect.NavigateToOcrProcessing })
    }

    @Test fun realScanOpensCreatedContractAfterUpload() = runTest {
        val requests = mutableListOf<DocumentUploadRequest>()
        val repository = object : DocumentUploadRepository {
            override fun upload(request: DocumentUploadRequest) = flowOf(
                ProcessingState.Running(progressPercent = 100),
                ProcessingState.Uploaded("15", "20")
            ).also { requests.add(request) }
        }
        val vm = UploadProgressViewModel(repository)
        val effects = mutableListOf<UploadProgressEffect>()
        backgroundScope.launch { vm.effects.collect { effects.add(it) } }
        val pages = listOf("file:///page-1.jpg", "file:///page-2.jpg")
        vm.onEvent(UploadProgressEvent.ScreenOpened(
            "scan.pdf", "PDF", documentId = "local-id", mimeType = "application/pdf",
            scanPageUris = pages, sessionKey = "session"
        ))
        runCurrent()

        assertEquals(listOf(DocumentUploadRequest("local-id", "scan.pdf", "application/pdf", pages, true, "session")), requests)
        assertEquals(listOf(UploadProgressEffect.NavigateToContract("15")), effects)
        assertEquals(100, vm.uiState.value.progress)
    }

    @Test fun uploadRejectionKeepsTypedReason() = runTest {
        val repository = object : DocumentUploadRepository {
            override fun upload(request: DocumentUploadRequest) =
                flowOf(ProcessingState.Failed(isRetryable = false, reason = UploadFailure.TOO_LARGE))
        }
        val vm = UploadProgressViewModel(repository)
        val effects = mutableListOf<UploadProgressEffect>()
        backgroundScope.launch { vm.effects.collect { effects.add(it) } }
        vm.onEvent(UploadProgressEvent.ScreenOpened("file", "pdf", "content://fixture", "doc-a"))
        runCurrent()

        assertEquals(listOf(UploadProgressEffect.NavigateToUploadError(UploadFailure.TOO_LARGE)), effects)
    }

    @Test fun cancelledProcessingDoesNotNavigateToOcr() = runTest {
        val vm = UploadProgressViewModel(com.example.lagallens.data.repository.FakeDocumentUploadRepository())
        val effects = mutableListOf<UploadProgressEffect>()
        backgroundScope.launch { vm.effects.collect { effects.add(it) } }
        runCurrent()
        vm.onEvent(UploadProgressEvent.ScreenOpened("file", "pdf", "content://fixture", "doc-a"))
        advanceTimeBy(400); runCurrent()
        vm.onEvent(UploadProgressEvent.CancelClicked)
        advanceTimeBy(2000); runCurrent()
        assertFalse(effects.any { it is UploadProgressEffect.NavigateToOcrProcessing })
    }
}
