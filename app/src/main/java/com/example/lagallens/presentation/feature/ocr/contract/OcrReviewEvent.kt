package com.example.lagallens.presentation.feature.ocr.contract

sealed interface OcrReviewEvent {
    data object BackClicked : OcrReviewEvent
    data class TextChanged(val text: String) : OcrReviewEvent
    data object RestoreOriginalClicked : OcrReviewEvent
    data object SaveClicked : OcrReviewEvent
}
