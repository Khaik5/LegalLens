package com.example.lagallens.presentation.feature.analysis.risk.findings.contract

import com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskFilter
import com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskFinding

data class AnalysisRiskFindingsUiState(
    val status: com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus = com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.LOADING,
    val documentName: String = "",
    val documentMetadata: String = "",
    val selectedFilter: AnalysisRiskFilter = AnalysisRiskFilter.ALL,
    val findings: List<AnalysisRiskFinding> = emptyList(),
    val lockedCount: Int = 0,
    /** Counts of the findings the user can see, whatever filter is selected; they label the filter tabs. */
    val allCount: Int = 0,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0
)
