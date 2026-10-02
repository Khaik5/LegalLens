package com.example.lagallens.presentation.feature.analysis.legalreferences.contract

import com.example.lagallens.presentation.feature.analysis.legalreferences.model.AnalysisLegalReference

data class AnalysisLegalReferencesUiState(
    val documentName: String = "",
    val documentMetadata: String = "",
    val query: String = "",
    val references: List<AnalysisLegalReference> = emptyList()
)
