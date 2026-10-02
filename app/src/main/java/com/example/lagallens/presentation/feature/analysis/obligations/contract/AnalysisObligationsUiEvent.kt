package com.example.lagallens.presentation.feature.analysis.obligations.contract

sealed interface AnalysisObligationsUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisObligationsUiEvent

    data object BackClicked : AnalysisObligationsUiEvent
}
