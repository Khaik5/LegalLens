package com.example.lagallens.domain.model.ocr

/** Why text recognition failed, as far as the backend says; decides what the user is told and whether a retry helps. */
enum class OcrFailureReason {
    /** The recognition service is down, misconfigured or answered badly; nothing is wrong with the document. */
    SERVICE_UNAVAILABLE,

    /** Recognition took too long. */
    TIMEOUT,

    /** The file is unsupported, too large, missing or was changed after it was uploaded. */
    FILE_PROBLEM,

    /** The document itself could not be read, or the backend gave no reason. */
    UNREADABLE;

    companion object {
        /** Maps the backend's job error code; an unknown or missing code reads as [UNREADABLE]. */
        fun fromCode(code: String?): OcrFailureReason = when (code) {
            "OCR_ENGINE_NOT_INSTALLED", "OCR_PROVIDER_ERROR", "OCR_PROVIDER_UNAVAILABLE", "OCR_PROVIDER_NOT_CONFIGURED",
            "OCR_HTTP_ERROR", "OCR_INVALID_RESULT", "OCR_LEASE_EXPIRED" -> SERVICE_UNAVAILABLE
            "OCR_PROCESSING_TIMEOUT" -> TIMEOUT
            "OCR_UNSUPPORTED_FILE", "OCR_SOURCE_MISSING", "OCR_SOURCE_CHANGED" -> FILE_PROBLEM
            else -> UNREADABLE
        }
    }
}
