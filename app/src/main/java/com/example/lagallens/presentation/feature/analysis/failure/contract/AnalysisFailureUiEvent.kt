package com.example.lagallens.presentation.feature.analysis.failure.contract

sealed interface AnalysisFailureUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisFailureUiEvent

    data object BackClicked : AnalysisFailureUiEvent
    data object RetryClicked : AnalysisFailureUiEvent
    data object ReturnHomeClicked : AnalysisFailureUiEvent
}
