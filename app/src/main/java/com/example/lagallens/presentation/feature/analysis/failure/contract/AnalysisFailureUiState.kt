package com.example.lagallens.presentation.feature.analysis.failure.contract

import androidx.annotation.StringRes
import com.example.lagallens.R

data class AnalysisFailureUiState(
    val documentName: String = "",
    val documentMetadata: String = "",
    @StringRes val messageRes: Int = R.string.analysis_failure_message
)

/** Backend codes for a job that failed because the AI service was busy or too slow, not because of the document. */
private val SERVICE_BUSY_CODES = setOf("ANALYSIS_PROVIDER_UNAVAILABLE", "ANALYSIS_RATE_LIMITED", "ANALYSIS_TIMEOUT")

@StringRes
fun analysisFailureMessageRes(errorCode: String?): Int =
    if (errorCode in SERVICE_BUSY_CODES) R.string.analysis_failure_message_service_busy else R.string.analysis_failure_message
