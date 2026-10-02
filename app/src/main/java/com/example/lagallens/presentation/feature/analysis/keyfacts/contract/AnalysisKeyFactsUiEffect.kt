package com.example.lagallens.presentation.feature.analysis.keyfacts.contract

sealed interface AnalysisKeyFactsUiEffect {
    data object NavigateBack : AnalysisKeyFactsUiEffect

    data class ShowClauseSource(
        val source: AnalysisClauseSource
    ) : AnalysisKeyFactsUiEffect
}
