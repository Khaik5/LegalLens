package com.example.lagallens.presentation.feature.ocr.contract

sealed interface OcrReviewEffect {
    data object NavigateBack : OcrReviewEffect
    data object ShowSaved : OcrReviewEffect
}
