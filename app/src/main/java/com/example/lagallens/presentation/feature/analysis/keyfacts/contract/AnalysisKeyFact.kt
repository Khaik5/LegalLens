package com.example.lagallens.presentation.feature.analysis.keyfacts.contract

import androidx.annotation.StringRes

data class AnalysisKeyFact(
    @StringRes val labelRes: Int,
    @StringRes val referenceRes: Int,
    @StringRes val valueRes: Int,
    @StringRes val descriptionRes: Int,
    val source: AnalysisClauseSource? = null
)
