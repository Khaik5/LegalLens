package com.example.lagallens.presentation.feature.ocr.result.contract

data class OcrResultUiState(
    val confidenceLabel: String = "",
    @androidx.annotation.StringRes val confidenceLabelRes: Int? = null,
    val confidencePercentage: String = "",
    val documentText: String = "",
    val documentName: String = "",
    val documentDetails: String = "",
    val documentId: String = "",
    val contractId: String? = null,
    val documentUri: String? = null,
    val mimeType: String = "",
    val warnings: List<com.example.lagallens.domain.model.ocr.OcrWarning> = emptyList(),
    val isSampleContent: Boolean = false,
    val isReviewConfirmed: Boolean = false,
    val errorMessageRes: Int? = null
)
