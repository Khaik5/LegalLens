package com.example.lagallens.presentation.feature.analysis.obligations.contract

data class AnalysisObligation(val id: String, val title: String, val clause: String, val description: String, val source: com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisClauseSource? = null)
