package com.example.lagallens.presentation.feature.camera.ocr.contract

sealed interface OcrProcessingUiEvent {
    data class ScreenOpened(val documentId: String, val pagePaths: List<String> = emptyList(), val contractId: String? = null) : OcrProcessingUiEvent

    /** The user leaves while recognition is still running, so the upload is abandoned. */
    data object BackClicked : OcrProcessingUiEvent
}
