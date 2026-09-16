package com.example.lagallens.presentation.feature.camera.ocr.contract

sealed interface OcrProcessingUiEvent {
    data class ScreenOpened(val shouldSimulateFailure: Boolean) : OcrProcessingUiEvent
}
