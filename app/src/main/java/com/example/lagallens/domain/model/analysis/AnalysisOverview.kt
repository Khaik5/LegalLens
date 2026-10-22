package com.example.lagallens.domain.model.analysis

data class AnalysisOverview(
    val score: Int,
    val keyFactsCount: Int,
    val riskCount: Int,
    val referenceCount: Int
)
