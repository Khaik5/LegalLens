package com.example.lagallens.domain.model.analysis

/** Identity of one completed local DEMO analysis. */
data class AnalysisIdentity(
    val resultId: String,
    val operationId: String,
    val contractId: String,
    val documentId: String,
    val findingIds: List<String> = emptyList(),
    val overview: AnalysisOverview? = null,
    val snapshot: Map<String, String>? = null
)
