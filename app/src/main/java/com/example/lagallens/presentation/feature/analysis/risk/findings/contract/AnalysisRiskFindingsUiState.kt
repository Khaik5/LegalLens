package com.example.lagallens.presentation.feature.analysis.risk.findings.contract

import com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskFilter
import com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskFinding

data class AnalysisRiskFindingsUiState(
    val status: com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus = com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.LOADING,
    val documentName: String = "",
    val documentMetadata: String = "",
    val selectedFilter: AnalysisRiskFilter = AnalysisRiskFilter.ALL,
    val findings: List<AnalysisRiskFinding> = emptyList(),
    val lockedCount: Int = 0
) {
}
