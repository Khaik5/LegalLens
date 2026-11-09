package com.example.lagallens.presentation.feature.analysis.keyfacts.contract

data class AnalysisKeyFactsUiState(
    val status: com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus = com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.LOADING,
    val documentName: String = "",
    val documentMetadata: String = "",
    val resultId: String? = null,
    val facts: List<AnalysisKeyFact> = emptyList(),
    val lockedCount: Int = 0
)
