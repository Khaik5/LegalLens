package com.example.lagallens.presentation.feature.analysis.start.contract

import androidx.annotation.StringRes

sealed interface AnalysisStartUiEffect {
    data object NavigateBack : AnalysisStartUiEffect

    data class ShowMessage(@StringRes val messageRes: Int) : AnalysisStartUiEffect

    data class NavigateToAnalysisProcessing(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisStartUiEffect
}
