package com.example.lagallens.presentation.feature.camera.ocr.contract

sealed interface OcrProcessingUiEffect {
    data class NavigateToOcrResult(
        val documentName: String,
        val documentDetails: String
    ) : OcrProcessingUiEffect

    data class NavigateToOcrFailure(
        val documentName: String,
        val documentDetails: String
    ) : OcrProcessingUiEffect
}
