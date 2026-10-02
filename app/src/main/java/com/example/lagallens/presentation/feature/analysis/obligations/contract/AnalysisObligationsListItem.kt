package com.example.lagallens.presentation.feature.analysis.obligations.contract

import androidx.annotation.StringRes

sealed interface AnalysisObligationsListItem {
    data class Section(
        @StringRes val titleRes: Int
    ) : AnalysisObligationsListItem

    data class Obligation(
        val obligation: AnalysisObligation
    ) : AnalysisObligationsListItem
}
