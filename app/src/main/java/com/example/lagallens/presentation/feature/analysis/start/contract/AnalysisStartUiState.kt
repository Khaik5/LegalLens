package com.example.lagallens.presentation.feature.analysis.start.contract

import com.example.lagallens.R

data class AnalysisStartUiState(
    val documentName: String = "",
    val documentMetadata: String = "",
    val benefits: List<AnalysisStartBenefit> = listOf(
        AnalysisStartBenefit(
            id = 1,
            stepRes = R.string.analysis_start_step_one,
            titleRes = R.string.analysis_start_summary_title,
            descriptionRes = R.string.analysis_start_summary_description
        ),
        AnalysisStartBenefit(
            id = 2,
            stepRes = R.string.analysis_start_step_two,
            titleRes = R.string.analysis_start_risk_title,
            descriptionRes = R.string.analysis_start_risk_description
        ),
        AnalysisStartBenefit(
            id = 3,
            stepRes = R.string.analysis_start_step_three,
            titleRes = R.string.analysis_start_legal_title,
            descriptionRes = R.string.analysis_start_legal_description
        )
    )
)
