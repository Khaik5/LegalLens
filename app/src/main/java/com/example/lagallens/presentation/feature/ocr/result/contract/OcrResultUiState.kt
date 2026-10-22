package com.example.lagallens.presentation.feature.ocr.result.contract

data class OcrResultUiState(
    val confidenceLabel: String = "",
    val confidencePercentage: String = "",
    val documentText: String = "",
    val documentName: String = "",
    val documentDetails: String = "",
    val isReviewConfirmed: Boolean = false
)
