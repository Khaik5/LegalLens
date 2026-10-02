package com.example.lagallens.presentation.feature.analysis.legalreferences.contract

sealed interface AnalysisLegalReferencesUiEffect {
    data object NavigateBack : AnalysisLegalReferencesUiEffect
    data object ShowDetailUnavailable : AnalysisLegalReferencesUiEffect

    data class NavigateToDetail(
        val referenceId: String,
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisLegalReferencesUiEffect
}
