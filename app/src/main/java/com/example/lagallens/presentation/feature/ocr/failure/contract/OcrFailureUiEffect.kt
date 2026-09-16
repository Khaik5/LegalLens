package com.example.lagallens.presentation.feature.ocr.failure.contract

sealed interface OcrFailureUiEffect {
    data object NavigateBack : OcrFailureUiEffect

    data class RetryOcr(
        val documentName: String,
        val documentDetails: String
    ) : OcrFailureUiEffect
}
