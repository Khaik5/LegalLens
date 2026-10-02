package com.example.lagallens.presentation.feature.analysis.risk.detail.contract

sealed interface AnalysisRiskDetailUiEvent {
    data object BackClicked : AnalysisRiskDetailUiEvent
    data object LegalReferenceClicked : AnalysisRiskDetailUiEvent
}
