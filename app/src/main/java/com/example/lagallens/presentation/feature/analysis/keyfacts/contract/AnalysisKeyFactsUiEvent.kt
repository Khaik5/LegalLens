package com.example.lagallens.presentation.feature.analysis.keyfacts.contract

sealed interface AnalysisKeyFactsUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisKeyFactsUiEvent

    data class SourceClicked(
        val source: AnalysisClauseSource
    ) : AnalysisKeyFactsUiEvent

    data object BackClicked : AnalysisKeyFactsUiEvent
}
