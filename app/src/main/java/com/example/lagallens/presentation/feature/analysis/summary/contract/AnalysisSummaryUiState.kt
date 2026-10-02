package com.example.lagallens.presentation.feature.analysis.summary.contract

import com.example.lagallens.R

data class AnalysisSummaryUiState(
    val documentName: String = "",
    val documentMetadata: String = "",
    val sections: List<AnalysisSummarySection> = listOf(
        AnalysisSummarySection(
            titleRes = R.string.analysis_summary_contract_type_title,
            descriptionRes = R.string.analysis_summary_contract_type_description
        ),
        AnalysisSummarySection(
            titleRes = R.string.analysis_summary_parties_title,
            descriptionRes = R.string.analysis_summary_parties_description
        ),
        AnalysisSummarySection(
            titleRes = R.string.analysis_summary_effective_term_title,
            descriptionRes = R.string.analysis_summary_effective_term_description
        ),
        AnalysisSummarySection(
            titleRes = R.string.analysis_summary_key_terms_title,
            descriptionRes = R.string.analysis_summary_key_terms_description
        ),
        AnalysisSummarySection(
            titleRes = R.string.analysis_summary_notable_points_title,
            descriptionRes = R.string.analysis_summary_notable_points_description
        )
    )
)
