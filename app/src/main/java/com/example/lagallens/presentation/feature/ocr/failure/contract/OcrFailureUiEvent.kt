package com.example.lagallens.presentation.feature.ocr.failure.contract

sealed interface OcrFailureUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentDetails: String
    ) : OcrFailureUiEvent

    data object BackClicked : OcrFailureUiEvent

    data object RetryClicked : OcrFailureUiEvent
}
