package com.example.lagallens.presentation.feature.analysis.start.contract

sealed interface AnalysisStartUiEffect {
    data object NavigateBack : AnalysisStartUiEffect

    data class NavigateToAnalysisProcessing(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisStartUiEffect
}
