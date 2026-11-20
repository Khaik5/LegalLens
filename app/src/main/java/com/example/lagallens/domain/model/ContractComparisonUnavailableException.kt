package com.example.lagallens.domain.model

enum class ContractComparisonBlocker { DOCUMENT_MISSING, OCR_NOT_READY, DOCUMENT_UNAVAILABLE, TOO_LARGE, NOT_FOUND, ACCESS_DENIED }

/** The backend refused the comparison for a reason the user can act on, so the screen explains it. */
class ContractComparisonUnavailableException(val blocker: ContractComparisonBlocker, cause: Throwable? = null) :
    Exception("Contract comparison is unavailable: $blocker", cause)
