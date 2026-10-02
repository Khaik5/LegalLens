package com.example.lagallens.presentation.feature.analysis.legalreferences.contract

sealed interface AnalysisLegalReferencesUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisLegalReferencesUiEvent

    data class QueryChanged(val query: String) : AnalysisLegalReferencesUiEvent
    data class ReferenceClicked(val referenceId: String) : AnalysisLegalReferencesUiEvent
    data object BackClicked : AnalysisLegalReferencesUiEvent
}
