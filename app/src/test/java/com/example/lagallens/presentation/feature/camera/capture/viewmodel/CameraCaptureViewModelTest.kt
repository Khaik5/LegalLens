package com.example.lagallens.presentation.feature.camera.capture.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.model.camera.DocumentDetection
import com.example.lagallens.domain.model.camera.DocumentQualityResult
import com.example.lagallens.domain.model.camera.QualityDecision
import com.example.lagallens.domain.model.camera.ScanPage
import com.example.lagallens.presentation.feature.camera.capture.contract.CameraCaptureUiEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CameraCaptureViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val ready = DocumentDetection.Ready(0.5, 0.5, 0.5)
    private val accepted = DocumentQualityResult(QualityDecision.ACCEPTED, emptyList())
    private val warning = DocumentQualityResult(QualityDecision.WARNING, emptyList())

    private fun CameraCaptureViewModel.showReadyFrames(count: Int = 12) =
        repeat(count) { onEvent(CameraCaptureUiEvent.DetectionUpdated(ready)) }

    @Test fun autoCaptureTakesOnePageUntilTheDocumentLeavesTheFrame() {
        val vm = CameraCaptureViewModel()
        vm.showReadyFrames()
        assertTrue(vm.uiState.value.isCapturing)
        vm.onEvent(CameraCaptureUiEvent.PhotoQualityChecked(ScanPage("first"), accepted))

        vm.showReadyFrames(24)
        assertFalse(vm.uiState.value.isCapturing)

        vm.onEvent(CameraCaptureUiEvent.DetectionUpdated(DocumentDetection.NotFound))
        vm.showReadyFrames()
        assertTrue(vm.uiState.value.isCapturing)
    }

    @Test fun everyWarnedPageKeepsItsOwnDecision() {
        val vm = CameraCaptureViewModel()
        vm.onEvent(CameraCaptureUiEvent.GalleryPagesQualityChecked(listOf(ScanPage("a") to warning, ScanPage("b") to warning)))
        assertEquals(2, vm.uiState.value.pendingQualityChecks.size)

        vm.onEvent(CameraCaptureUiEvent.KeepWarningPage("a"))
        vm.onEvent(CameraCaptureUiEvent.KeepWarningPage("b"))

        assertEquals(listOf("a", "b"), vm.uiState.value.pages.map { it.path })
        assertTrue(vm.uiState.value.pendingQualityChecks.isEmpty())
    }

    @Test fun manualShutterCapturesWithoutADetectedDocument() {
        val vm = CameraCaptureViewModel()
        vm.onEvent(CameraCaptureUiEvent.DetectionUpdated(DocumentDetection.NotFound))
        vm.onEvent(CameraCaptureUiEvent.ShutterClicked)
        assertTrue(vm.uiState.value.isCapturing)
    }

    @Test fun autoCaptureStillWaitsForADetectedDocument() {
        val vm = CameraCaptureViewModel()
        repeat(24) { vm.onEvent(CameraCaptureUiEvent.DetectionUpdated(DocumentDetection.NotFound)) }
        assertFalse(vm.uiState.value.isCapturing)
    }

    @Test fun pendingDecisionPausesAutoCapture() {
        val vm = CameraCaptureViewModel()
        vm.onEvent(CameraCaptureUiEvent.GalleryPagesQualityChecked(listOf(ScanPage("a") to warning)))
        vm.showReadyFrames(24)
        assertFalse(vm.uiState.value.isCapturing)
    }
}
