package com.example.lagallens.presentation.feature.analysis.summary.contract

sealed interface AnalysisSummaryUiEffect {
    data object NavigateBack : AnalysisSummaryUiEffect

    data class NavigateToKeyFacts(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisSummaryUiEffect
}
