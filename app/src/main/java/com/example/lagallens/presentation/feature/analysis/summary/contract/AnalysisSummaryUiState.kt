package com.example.lagallens.presentation.feature.analysis.summary.contract

data class AnalysisSummaryUiState(
    val status: com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus = com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.LOADING,
    val documentName: String = "",
    val documentMetadata: String = "",
    val resultId: String? = null,
    val sections: List<AnalysisSummarySection> = emptyList(),
    val lockedCount: Int = 0
)
