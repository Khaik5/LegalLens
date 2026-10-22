package com.example.lagallens.presentation.feature.analysis.risk.findings.contract

import com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskFilter

sealed interface AnalysisRiskFindingsUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentMetadata: String
    ) : AnalysisRiskFindingsUiEvent

    data object BackClicked : AnalysisRiskFindingsUiEvent

    data class FilterClicked(val filter: AnalysisRiskFilter) : AnalysisRiskFindingsUiEvent

    data class FindingClicked(val findingId: String) : AnalysisRiskFindingsUiEvent
}
