package com.example.lagallens.presentation.feature.analysis.result.contract

sealed interface AnalysisResultUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisResultUiEvent

    data class ItemClicked(
        val destination: AnalysisResultDestination
    ) : AnalysisResultUiEvent

    data object BackClicked : AnalysisResultUiEvent
}
