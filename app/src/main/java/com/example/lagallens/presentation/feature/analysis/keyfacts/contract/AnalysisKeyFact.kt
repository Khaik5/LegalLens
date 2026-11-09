package com.example.lagallens.presentation.feature.analysis.keyfacts.contract

data class AnalysisKeyFact(val id: String, val label: String, val reference: String, val value: String, val description: String, val source: AnalysisClauseSource? = null)
