package com.example.lagallens.presentation.feature.ocr.result.contract

data class OcrResultUiState(
    val confidenceLabel: String = "",
    val confidencePercentage: String = "",
    val documentText: String = "",
    val documentName: String = "",
    val documentDetails: String = "",
    val documentId: String = "",
    val contractId: String? = null,
    val documentUri: String? = null,
    val isReviewConfirmed: Boolean = false,
    val errorMessageRes: Int? = null
)
