package com.example.lagallens.presentation.feature.analysis.start.contract

import androidx.annotation.StringRes

data class AnalysisStartBenefit(
    val id: Int,
    @StringRes val stepRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)
