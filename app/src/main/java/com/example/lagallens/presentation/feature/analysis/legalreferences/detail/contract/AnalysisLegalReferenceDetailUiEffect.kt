package com.example.lagallens.presentation.feature.analysis.legalreferences.detail.contract

sealed interface AnalysisLegalReferenceDetailUiEffect {
    data object NavigateBack : AnalysisLegalReferenceDetailUiEffect
    data object NavigateToRelatedRisk : AnalysisLegalReferenceDetailUiEffect
}
