package com.example.lagallens.presentation.feature.camera.ocr.contract

import com.example.lagallens.domain.model.ocr.OcrFailureReason

sealed interface OcrProcessingUiEffect {
    data class NavigateToOcrResult(
        val documentName: String,
        val documentDetails: String
    ) : OcrProcessingUiEffect

    /** [reason] and [isRetryable] come from the backend's job result; the defaults fit a failure with no named cause. */
    data class NavigateToOcrFailure(
        val documentName: String,
        val documentDetails: String,
        val reason: OcrFailureReason = OcrFailureReason.UNREADABLE,
        val isRetryable: Boolean = true
    ) : OcrProcessingUiEffect
}
