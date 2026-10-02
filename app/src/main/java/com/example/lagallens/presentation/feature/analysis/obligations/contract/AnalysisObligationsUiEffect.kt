package com.example.lagallens.presentation.feature.analysis.obligations.contract

sealed interface AnalysisObligationsUiEffect {
    data object NavigateBack : AnalysisObligationsUiEffect
}
