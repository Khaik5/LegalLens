package com.example.lagallens.presentation.feature.camera.ocr.contract

sealed interface OcrProcessingUiEvent {
    data class ScreenOpened(val documentId: String, val pagePaths: List<String> = emptyList()) : OcrProcessingUiEvent
}
