package com.example.lagallens.presentation.feature.analysis.risk.contract

data class AnalysisRiskOverviewUiState(
    val documentName: String = "",
    val documentMetadata: String = "",
    val score: Int = 65,
    val highRiskCount: Int = 1,
    val mediumRiskCount: Int = 1,
    val lowRiskCount: Int = 1
)
