package com.example.lagallens.presentation.feature.analysis.processing.contract

sealed interface AnalysisProcessingUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentMetadata: String,
        val operationId: String,
        val contractId: String,
        val documentId: String,
        val sessionKey: String
    ) : AnalysisProcessingUiEvent
}
