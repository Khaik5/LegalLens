package com.example.lagallens.presentation.feature.analysis.risk.findings.contract

import androidx.annotation.StringRes

sealed interface AnalysisRiskFindingsUiEffect {
    data object NavigateBack : AnalysisRiskFindingsUiEffect

    data class NavigateToRiskDetail(val findingId: String) : AnalysisRiskFindingsUiEffect

    data class ShowMessage(@StringRes val messageRes: Int) : AnalysisRiskFindingsUiEffect
}
