package com.example.lagallens.data.model.response

data class OcrJobResponse(val jobId: String, val status: String)

/**
 * Latest OCR job of a document; [revision], [text] and [confidence] stay null until the job completes.
 * [confidence] belongs to the latest revision, so a user correction has none; [originalConfidence] keeps
 * the recognizer's value and is null on backends that do not send it yet.
 */
data class OcrResultResponse(
    val jobId: String,
    val status: String,
    val progress: Int?,
    val errorCode: String?,
    val retryable: Boolean?,
    val revision: Int?,
    val text: String?,
    val confidence: Double?,
    val pages: List<OcrPageResponse>?,
    val originalConfidence: Double? = null
)

data class OcrPageResponse(val page: Int, val text: String?, val confidence: Double?)

data class OcrCorrectionResponse(val jobId: String, val revision: Int, val text: String)
