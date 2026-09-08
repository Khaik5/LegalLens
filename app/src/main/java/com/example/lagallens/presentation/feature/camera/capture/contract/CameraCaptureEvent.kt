package com.example.lagallens.presentation.feature.camera.capture.contract

import com.example.lagallens.presentation.feature.camera.core.DocumentDetection
import com.example.lagallens.presentation.feature.camera.core.DocumentQualityResult
import com.example.lagallens.presentation.feature.camera.core.ScanPage

sealed interface CameraCaptureEvent {
    data object CloseClicked : CameraCaptureEvent
    data object FlashClicked : CameraCaptureEvent
    data object SettingsClicked : CameraCaptureEvent
    data object LibraryClicked : CameraCaptureEvent
    data object ShutterClicked : CameraCaptureEvent
    data class DetectionUpdated(val detection: DocumentDetection) : CameraCaptureEvent
    data class PhotoQualityChecked(
        val page: ScanPage,
        val qualityResult: DocumentQualityResult
    ) : CameraCaptureEvent
    data object CaptureFailed : CameraCaptureEvent
    data class GalleryPagesQualityChecked(val pages: List<Pair<ScanPage, DocumentQualityResult>>) : CameraCaptureEvent
    data object KeepWarningPage : CameraCaptureEvent
    data object DiscardWarningPage : CameraCaptureEvent
    data class PagesUpdated(val pages: List<ScanPage>) : CameraCaptureEvent
    data object ContinueClicked : CameraCaptureEvent
}
