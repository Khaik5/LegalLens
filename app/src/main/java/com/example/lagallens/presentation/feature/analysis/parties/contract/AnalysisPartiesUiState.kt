package com.example.lagallens.presentation.feature.analysis.parties.contract

data class AnalysisPartiesUiState(
    val status: com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus = com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.LOADING,
    val documentName: String = "",
    val documentMetadata: String = "",
    val resultId: String? = null,
    val parties: List<AnalysisParty> = emptyList(),
    val lockedCount: Int = 0
)
