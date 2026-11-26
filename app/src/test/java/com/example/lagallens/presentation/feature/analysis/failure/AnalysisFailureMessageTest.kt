package com.example.lagallens.presentation.feature.analysis.failure

import com.example.lagallens.R
import com.example.lagallens.presentation.feature.analysis.failure.contract.analysisFailureMessageRes
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisFailureMessageTest {
    @Test fun aBusyAiServiceIsNotBlamedOnTheDocument() {
        listOf("ANALYSIS_PROVIDER_UNAVAILABLE", "ANALYSIS_RATE_LIMITED", "ANALYSIS_TIMEOUT").forEach {
            assertEquals(it, R.string.analysis_failure_message_service_busy, analysisFailureMessageRes(it))
        }
    }

    @Test fun anUnknownOrMissingCodeUsesTheGenericMessage() {
        assertEquals(R.string.analysis_failure_message, analysisFailureMessageRes(null))
        assertEquals(R.string.analysis_failure_message, analysisFailureMessageRes("ANALYSIS_INVALID_RESULT"))
    }
}
