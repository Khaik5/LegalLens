package com.example.lagallens.domain.model.ocr

data class OcrResult(
    val confidenceLabel: String,
    val confidencePercentage: String,
    val documentText: String
)
