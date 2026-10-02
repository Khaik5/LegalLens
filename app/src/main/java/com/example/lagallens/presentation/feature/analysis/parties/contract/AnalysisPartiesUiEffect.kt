package com.example.lagallens.presentation.feature.analysis.parties.contract

sealed interface AnalysisPartiesUiEffect {
    data object NavigateBack : AnalysisPartiesUiEffect
}
