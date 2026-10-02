package com.example.lagallens.presentation.feature.analysis.keyfacts.contract

import com.example.lagallens.R

data class AnalysisKeyFactsUiState(
    val documentName: String = "",
    val documentMetadata: String = "",
    val facts: List<AnalysisKeyFact> = listOf(
        AnalysisKeyFact(
            labelRes = R.string.analysis_key_facts_salary_label,
            referenceRes = R.string.analysis_key_facts_salary_reference,
            valueRes = R.string.analysis_key_facts_salary_value,
            descriptionRes = R.string.analysis_key_facts_salary_description,
            source = AnalysisClauseSource(
                referenceRes = R.string.analysis_key_facts_salary_reference,
                titleRes = R.string.analysis_key_facts_salary_label,
                excerptRes = R.string.analysis_clause_source_salary_excerpt
            )
        ),
        AnalysisKeyFact(
            labelRes = R.string.analysis_key_facts_probation_label,
            referenceRes = R.string.analysis_key_facts_probation_reference,
            valueRes = R.string.analysis_key_facts_probation_value,
            descriptionRes = R.string.analysis_key_facts_probation_description
        ),
        AnalysisKeyFact(
            labelRes = R.string.analysis_key_facts_payment_label,
            referenceRes = R.string.analysis_key_facts_payment_reference,
            valueRes = R.string.analysis_key_facts_payment_value,
            descriptionRes = R.string.analysis_key_facts_payment_description
        ),
        AnalysisKeyFact(
            labelRes = R.string.analysis_key_facts_compensation_label,
            referenceRes = R.string.analysis_key_facts_compensation_reference,
            valueRes = R.string.analysis_key_facts_compensation_value,
            descriptionRes = R.string.analysis_key_facts_compensation_description
        ),
        AnalysisKeyFact(
            labelRes = R.string.analysis_key_facts_term_label,
            referenceRes = R.string.analysis_key_facts_term_reference,
            valueRes = R.string.analysis_key_facts_term_value,
            descriptionRes = R.string.analysis_key_facts_term_description
        ),
        AnalysisKeyFact(
            labelRes = R.string.analysis_key_facts_effective_date_label,
            referenceRes = R.string.analysis_key_facts_effective_date_reference,
            valueRes = R.string.analysis_key_facts_effective_date_value,
            descriptionRes = R.string.analysis_key_facts_effective_date_description
        )
    )
)
