package com.example.lagallens.presentation.feature.ocr.saved.contract

sealed interface OcrSavedUiEffect {
    data object NavigateBack : OcrSavedUiEffect

    data class NavigateToAnalysis(val documentName: String) : OcrSavedUiEffect
}
