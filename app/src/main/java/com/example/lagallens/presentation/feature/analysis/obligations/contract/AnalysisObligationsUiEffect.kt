package com.example.lagallens.presentation.feature.analysis.obligations.contract

sealed interface AnalysisObligationsUiEffect {
    data object NavigateBack : AnalysisObligationsUiEffect
    data class ShowSource(val source: com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisClauseSource) : AnalysisObligationsUiEffect
    data object SourceUnavailable : AnalysisObligationsUiEffect
}
