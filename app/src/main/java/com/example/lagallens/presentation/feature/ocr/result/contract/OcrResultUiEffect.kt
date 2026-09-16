package com.example.lagallens.presentation.feature.ocr.result.contract

sealed interface OcrResultUiEffect {
    data object NavigateBack : OcrResultUiEffect
    data class NavigateToEdit(val documentText: String) : OcrResultUiEffect
    data object NavigateToReview : OcrResultUiEffect
    data class NavigateToSaved(val documentName: String) : OcrResultUiEffect
}
