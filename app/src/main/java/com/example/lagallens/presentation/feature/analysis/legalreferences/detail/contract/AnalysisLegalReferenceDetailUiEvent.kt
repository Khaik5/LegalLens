package com.example.lagallens.presentation.feature.analysis.legalreferences.detail.contract

sealed interface AnalysisLegalReferenceDetailUiEvent {
    data class ScreenOpened(
        val referenceId: String,
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisLegalReferenceDetailUiEvent

    data object BackClicked : AnalysisLegalReferenceDetailUiEvent
    data object RelatedRiskClicked : AnalysisLegalReferenceDetailUiEvent
}
