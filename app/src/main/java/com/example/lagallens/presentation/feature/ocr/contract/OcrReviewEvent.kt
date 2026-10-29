package com.example.lagallens.presentation.feature.ocr.contract

sealed interface OcrReviewEvent {
    data class ScreenOpened(val documentId: String, val contractId: String?, val documentText: String) : OcrReviewEvent
    data class DocumentTextChanged(val documentText: String) : OcrReviewEvent
    data object BackClicked : OcrReviewEvent
    data object ConfirmClicked : OcrReviewEvent
}
