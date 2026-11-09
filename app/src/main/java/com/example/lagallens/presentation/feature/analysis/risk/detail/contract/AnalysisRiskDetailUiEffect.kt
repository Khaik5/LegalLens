package com.example.lagallens.presentation.feature.analysis.risk.detail.contract

sealed interface AnalysisRiskDetailUiEffect {
    data object NavigateBack : AnalysisRiskDetailUiEffect
    data object NavigateToCitation : AnalysisRiskDetailUiEffect
    data object CitationUnavailable : AnalysisRiskDetailUiEffect
}
