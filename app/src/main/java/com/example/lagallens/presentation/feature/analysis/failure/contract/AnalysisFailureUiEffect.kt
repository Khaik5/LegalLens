package com.example.lagallens.presentation.feature.analysis.failure.contract

sealed interface AnalysisFailureUiEffect {
    data object NavigateBack : AnalysisFailureUiEffect
    data object NavigateToHome : AnalysisFailureUiEffect

    data class RetryAnalysis(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisFailureUiEffect
}
