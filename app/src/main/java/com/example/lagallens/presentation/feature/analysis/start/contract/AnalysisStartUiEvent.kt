package com.example.lagallens.presentation.feature.analysis.start.contract

sealed interface AnalysisStartUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisStartUiEvent

    data object BackClicked : AnalysisStartUiEvent

    data object StartAnalysisClicked : AnalysisStartUiEvent
}
