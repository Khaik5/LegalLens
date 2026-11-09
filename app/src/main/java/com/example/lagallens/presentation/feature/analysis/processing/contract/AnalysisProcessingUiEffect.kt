package com.example.lagallens.presentation.feature.analysis.processing.contract

sealed interface AnalysisProcessingUiEffect {
    data class NavigateToAnalysisResult(
        val documentName: String,
        val documentMetadata: String,
        val resultId: String,
        val sessionKey: String
    ) : AnalysisProcessingUiEffect

    data class NavigateToAnalysisFailure(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisProcessingUiEffect
}
