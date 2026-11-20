package com.example.lagallens.presentation.feature.analysis.risk.contract

data class AnalysisRiskOverviewUiState(
    val documentName: String = "",
    val documentMetadata: String = "",
    val score: Int? = null,
    val highestSeverity: com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskSeverity? = null,
    val highRiskCount: Int = 0,
    val mediumRiskCount: Int = 0,
    val lowRiskCount: Int = 0,
    /** Every finding of the result, including the ones still locked. */
    val totalRiskCount: Int = 0,
    /** Findings hidden until the result is unlocked; the severity counts above cover only the visible ones. */
    val lockedRiskCount: Int = 0
)
