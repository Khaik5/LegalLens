package com.example.lagallens.presentation.feature.analysis.legalreferences.model

import androidx.annotation.StringRes

data class AnalysisLegalReference(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val countryRes: Int,
    @StringRes val articleRes: Int,
    @StringRes val topicRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val effectiveDateRes: Int,
    val searchTerms: String
)
