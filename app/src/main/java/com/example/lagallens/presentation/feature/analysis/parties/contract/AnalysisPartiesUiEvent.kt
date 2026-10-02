package com.example.lagallens.presentation.feature.analysis.parties.contract

sealed interface AnalysisPartiesUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisPartiesUiEvent

    data object BackClicked : AnalysisPartiesUiEvent
}
