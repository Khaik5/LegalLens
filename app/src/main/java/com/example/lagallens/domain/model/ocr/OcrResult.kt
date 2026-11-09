package com.example.lagallens.domain.model.ocr

data class OcrResult(
    val confidenceLabel: String,
    val confidencePercentage: String,
    val documentText: String,
    val warnings: List<OcrWarning> = emptyList(),
    /** True when the text did not come from the user's file. */
    val isSampleContent: Boolean = false
)

data class OcrWarning(
    val section: String,
    val confidence: String,
    val recognizedValue: String,
    val source: String
)
