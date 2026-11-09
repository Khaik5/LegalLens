package com.example.lagallens.presentation.feature.analysis.risk.contract

data class AnalysisRiskOverviewUiState(
    val documentName: String = "",
    val documentMetadata: String = "",
    val score: Int? = null,
    val highestSeverity: com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskSeverity? = null,
    val highRiskCount: Int = 0,
    val mediumRiskCount: Int = 0,
    val lowRiskCount: Int = 0
)
