package com.example.lagallens.presentation.feature.analysis.result.contract

sealed interface AnalysisResultUiEffect {
    data object NavigateBack : AnalysisResultUiEffect

    data class NavigateToSummary(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisResultUiEffect

    data class NavigateToKeyFacts(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisResultUiEffect

    data class NavigateToParties(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisResultUiEffect

    data class NavigateToObligations(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisResultUiEffect

    data class NavigateToRiskOverview(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisResultUiEffect

    data class NavigateToLegalReferences(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisResultUiEffect
}
