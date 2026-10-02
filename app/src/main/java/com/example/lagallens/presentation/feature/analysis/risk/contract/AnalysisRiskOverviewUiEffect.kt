package com.example.lagallens.presentation.feature.analysis.risk.contract

sealed interface AnalysisRiskOverviewUiEffect {
    data object NavigateBack : AnalysisRiskOverviewUiEffect

    data class NavigateToFindings(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisRiskOverviewUiEffect
}
