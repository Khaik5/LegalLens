package com.example.lagallens.presentation.feature.analysis.obligations.contract

sealed interface AnalysisObligationsUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisObligationsUiEvent

    data object BackClicked : AnalysisObligationsUiEvent
    data class TabSelected(val tab: com.example.lagallens.domain.model.analysis.AnalysisObligationTab) : AnalysisObligationsUiEvent
    data class SourceClicked(val id: String) : AnalysisObligationsUiEvent
}
