package com.example.lagallens.presentation.feature.analysis.result.contract

import com.example.lagallens.R

data class AnalysisResultUiState(
    val documentName: String = "",
    val documentMetadata: String = "",
    val score: Int = 65,
    val keyFactsCount: Int = 8,
    val riskCount: Int = 3,
    val referenceCount: Int = 12,
    val items: List<AnalysisResultItem> = listOf(
        AnalysisResultItem(
            iconRes = R.drawable.ic_ocr_saved_document,
            titleRes = R.string.analysis_result_summary_title,
            descriptionRes = R.string.analysis_result_summary_description,
            destination = AnalysisResultDestination.SUMMARY
        ),
        AnalysisResultItem(
            iconRes = R.drawable.ic_notification_info,
            titleRes = R.string.analysis_result_important_information_title,
            descriptionRes = R.string.analysis_result_important_information_description,
            destination = AnalysisResultDestination.KEY_FACTS
        ),
        AnalysisResultItem(
            iconRes = R.drawable.ic_analysis_result_users,
            titleRes = R.string.analysis_result_parties_title,
            descriptionRes = R.string.analysis_result_parties_description,
            destination = AnalysisResultDestination.PARTIES
        ),
        AnalysisResultItem(
            iconRes = R.drawable.ic_shield_check,
            titleRes = R.string.analysis_result_rights_title,
            descriptionRes = R.string.analysis_result_rights_description,
            destination = AnalysisResultDestination.OBLIGATIONS
        ),
        AnalysisResultItem(
            iconRes = R.drawable.ic_alert_triangle,
            titleRes = R.string.analysis_result_risk_title,
            descriptionRes = R.string.analysis_result_risk_description,
            isRisk = true,
            destination = AnalysisResultDestination.RISK_OVERVIEW
        ),
        AnalysisResultItem(
            iconRes = R.drawable.ic_analysis_result_book,
            titleRes = R.string.analysis_result_legal_basis_title,
            descriptionRes = R.string.analysis_result_legal_basis_description,
            destination = AnalysisResultDestination.LEGAL_REFERENCES
        )
    )
)
