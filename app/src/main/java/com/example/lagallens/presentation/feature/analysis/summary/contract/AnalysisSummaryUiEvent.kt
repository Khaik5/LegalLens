package com.example.lagallens.presentation.feature.analysis.summary.contract

sealed interface AnalysisSummaryUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisSummaryUiEvent

    data object BackClicked : AnalysisSummaryUiEvent

    data object ImportantInformationClicked : AnalysisSummaryUiEvent
}
