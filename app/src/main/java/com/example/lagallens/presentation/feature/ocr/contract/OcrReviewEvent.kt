package com.example.lagallens.presentation.feature.ocr.contract

sealed interface OcrReviewEvent {
    data object BackClicked : OcrReviewEvent
    data object ConfirmClicked : OcrReviewEvent
}
