package com.example.lagallens.presentation.feature.analysis.result.contract

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

enum class AnalysisResultDestination {
    SUMMARY,
    KEY_FACTS,
    PARTIES,
    OBLIGATIONS,
    RISK_OVERVIEW,
    LEGAL_REFERENCES
}

data class AnalysisResultItem(
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val isRisk: Boolean = false,
    val destination: AnalysisResultDestination? = null
)
