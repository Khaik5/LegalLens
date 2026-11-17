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
    val originalConfidence: Double? = null,
    /** Identifies the latest revision; analysis jobs are created from it. Null on older backends. */
    val revisionId: String? = null,
    /** PENDING until the owner confirms the revision, then APPROVED. */
    val reviewStatus: String? = null
)

data class OcrConfirmResponse(val revisionId: String, val revision: Int, val reviewStatus: String?)

data class OcrPageResponse(val page: Int, val text: String?, val confidence: Double?)

data class OcrCorrectionResponse(val jobId: String, val revision: Int, val text: String)
