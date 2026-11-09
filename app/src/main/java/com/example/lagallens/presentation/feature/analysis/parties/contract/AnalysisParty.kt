package com.example.lagallens.presentation.feature.analysis.parties.contract

data class AnalysisPartyField(val id: String, val label: String, val value: String)
data class AnalysisParty(val id: String, val title: String, val role: String, val fields: List<AnalysisPartyField>)
