package com.example.lagallens.presentation.feature.upload.progress.viewmodel

import com.example.lagallens.MainDispatcherRule
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
