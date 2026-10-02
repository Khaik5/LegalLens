package com.example.lagallens.presentation.feature.analysis.obligations.contract

import com.example.lagallens.R

data class AnalysisObligationsUiState(
    val documentName: String = "",
    val documentMetadata: String = "",
    val items: List<AnalysisObligationsListItem> = listOf(
        AnalysisObligationsListItem.Section(R.string.analysis_obligations_employer_heading),
        AnalysisObligationsListItem.Obligation(
            AnalysisObligation(
                titleRes = R.string.analysis_obligations_working_conditions_title,
                clauseRes = R.string.analysis_obligations_working_conditions_clause,
                descriptionRes = R.string.analysis_obligations_working_conditions_description
            )
        ),
        AnalysisObligationsListItem.Obligation(
            AnalysisObligation(
                titleRes = R.string.analysis_obligations_salary_title,
                clauseRes = R.string.analysis_obligations_salary_clause,
                descriptionRes = R.string.analysis_obligations_salary_description
            )
        ),
        AnalysisObligationsListItem.Obligation(
            AnalysisObligation(
                titleRes = R.string.analysis_obligations_insurance_title,
                clauseRes = R.string.analysis_obligations_insurance_clause,
                descriptionRes = R.string.analysis_obligations_insurance_description
            )
        ),
        AnalysisObligationsListItem.Section(R.string.analysis_obligations_employee_heading),
        AnalysisObligationsListItem.Obligation(
            AnalysisObligation(
                titleRes = R.string.analysis_obligations_professional_work_title,
                clauseRes = R.string.analysis_obligations_professional_work_clause,
                descriptionRes = R.string.analysis_obligations_professional_work_description
            )
        ),
        AnalysisObligationsListItem.Obligation(
            AnalysisObligation(
                titleRes = R.string.analysis_obligations_confidentiality_title,
                clauseRes = R.string.analysis_obligations_confidentiality_clause,
                descriptionRes = R.string.analysis_obligations_confidentiality_description
            )
        )
    )
)
