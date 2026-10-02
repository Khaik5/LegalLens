package com.example.lagallens.presentation.feature.analysis.risk.contract

sealed interface AnalysisRiskOverviewUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisRiskOverviewUiEvent

    data object BackClicked : AnalysisRiskOverviewUiEvent

    data object ViewDetailsClicked : AnalysisRiskOverviewUiEvent
}
