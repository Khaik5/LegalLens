package com.example.lagallens.presentation.feature.analysis.obligations.contract

data class AnalysisObligationsUiState(
    val status: com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus = com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.LOADING,
    val documentName: String = "",
    val documentMetadata: String = "",
    val resultId: String? = null,
    val items: List<AnalysisObligationsListItem> = emptyList(),
    val selectedTab: com.example.lagallens.domain.model.analysis.AnalysisObligationTab = com.example.lagallens.domain.model.analysis.AnalysisObligationTab.RIGHTS,
    val lockedCount: Int = 0
)
