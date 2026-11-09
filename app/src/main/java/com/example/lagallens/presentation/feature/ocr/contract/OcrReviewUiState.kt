package com.example.lagallens.presentation.feature.ocr.contract

data class OcrReviewUiState(
    val warnings: List<OcrReviewWarning> = emptyList(),
    val documentId: String = "",
    val contractId: String? = null,
    val documentText: String = ""
)

data class OcrReviewWarning(
    val section: String,
    val confidence: String,
    val recognizedValue: String,
    val source: String
)
