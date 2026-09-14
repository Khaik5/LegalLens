package com.example.lagallens.presentation.feature.camera.capture.contract

import com.example.lagallens.domain.model.camera.DocumentDetection
import com.example.lagallens.domain.model.camera.DocumentQualityResult
import com.example.lagallens.domain.model.camera.ScanPage

sealed interface CameraCaptureUiEvent {
    data object CloseClicked : CameraCaptureUiEvent
    data object FlashClicked : CameraCaptureUiEvent
    data object SettingsClicked : CameraCaptureUiEvent
    data object LibraryClicked : CameraCaptureUiEvent
    data object ShutterClicked : CameraCaptureUiEvent
    data class DetectionUpdated(val detection: DocumentDetection) : CameraCaptureUiEvent
    data class PhotoQualityChecked(
        val page: ScanPage,
        val qualityResult: DocumentQualityResult
    ) : CameraCaptureUiEvent
    data object CaptureFailed : CameraCaptureUiEvent
    data class GalleryPagesQualityChecked(val pages: List<Pair<ScanPage, DocumentQualityResult>>) : CameraCaptureUiEvent
    data object KeepWarningPage : CameraCaptureUiEvent
    data object DiscardWarningPage : CameraCaptureUiEvent
    data class PagesUpdated(val pages: List<ScanPage>) : CameraCaptureUiEvent
    data object ContinueClicked : CameraCaptureUiEvent
}
