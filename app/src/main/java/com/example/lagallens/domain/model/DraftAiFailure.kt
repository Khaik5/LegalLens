package com.example.lagallens.domain.model

/** Why a draft request to the AI did not produce text, so the screen can say what to do next. */
enum class DraftAiFailureReason {
    /** The AI did not answer in time; asking again may work. */
    TIMEOUT,

    /** The AI service is busy; asking again later may work. */
    OVERLOADED,

    /** Personal details could not be protected or restored safely, so nothing was sent or applied. */
    PRIVACY_PROTECTION,

    /** The text of the source contract is not available, usually because its OCR is not confirmed. */
    SOURCE_UNAVAILABLE,

    /** The draft changed after the request was made; its latest version must be opened first. */
    OUT_OF_DATE,

    OTHER
}

/** A draft AI request that failed for a known [reason]; other failures (for example no network) stay as they are. */
class DraftAiFailedException(val reason: DraftAiFailureReason) : IllegalStateException("Draft AI request failed: $reason")
