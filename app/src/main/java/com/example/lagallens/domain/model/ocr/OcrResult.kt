package com.example.lagallens.domain.model.ocr

data class OcrResult(
    val confidenceLabel: String,
    val confidencePercentage: String,
    val documentText: String,
    val warnings: List<OcrWarning> = emptyList(),
    /** True when the text did not come from the user's file. */
    val isSampleContent: Boolean = false,
    /** Average recognition confidence from 0 to 1, when the recognizer reports one. */
    val confidence: Double? = null
)

data class OcrWarning(
    val section: String,
    val confidence: String,
    val recognizedValue: String,
    val source: String
)
