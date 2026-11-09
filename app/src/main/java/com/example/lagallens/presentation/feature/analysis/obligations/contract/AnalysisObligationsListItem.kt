package com.example.lagallens.presentation.feature.analysis.obligations.contract

sealed interface AnalysisObligationsListItem {
    data class Section(val id: String, val title: String) : AnalysisObligationsListItem
    data class Obligation(val obligation: AnalysisObligation) : AnalysisObligationsListItem
}
