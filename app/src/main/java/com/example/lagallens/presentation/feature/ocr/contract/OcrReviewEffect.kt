package com.example.lagallens.presentation.feature.ocr.contract

sealed interface OcrReviewEffect {
    data object NavigateBack : OcrReviewEffect
    data class FinishWithConfirmedReview(val documentId: String, val contractId: String?, val documentText: String) : OcrReviewEffect
}
