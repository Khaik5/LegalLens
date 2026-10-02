package com.example.lagallens.presentation.feature.analysis.risk.citation.contract

sealed interface AnalysisRiskCitationUiEvent {
    data object BackClicked : AnalysisRiskCitationUiEvent
    data object ReturnToRiskClicked : AnalysisRiskCitationUiEvent
}
