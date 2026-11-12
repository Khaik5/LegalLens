package com.example.lagallens.presentation.feature.ocr.result.contract

sealed interface OcrResultUiEffect {
    data object NavigateBack : OcrResultUiEffect
    data class NavigateToEdit(val documentText: String) : OcrResultUiEffect
    data object NavigateToReview : OcrResultUiEffect
    data class NavigateToSaved(val documentName: String, val documentId: String, val contractId: String) : OcrResultUiEffect
    data class NavigateToOcrProcessing(
        val documentName: String,
        val documentDetails: String,
        val documentId: String,
        val contractId: String?,
        val mimeType: String
    ) : OcrResultUiEffect
}
