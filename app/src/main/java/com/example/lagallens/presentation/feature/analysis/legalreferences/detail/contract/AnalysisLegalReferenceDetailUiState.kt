package com.example.lagallens.presentation.feature.analysis.legalreferences.detail.contract

import androidx.annotation.StringRes
import com.example.lagallens.R

data class AnalysisLegalReferenceDetailUiState(
    val referenceId: String = "",
    val documentName: String = "",
    val documentMetadata: String = "",
    val isDetailedExample: Boolean = true,
    @StringRes val titleRes: Int = R.string.analysis_legal_references_commercial_law_title,
    @StringRes val articleRes: Int = R.string.analysis_legal_reference_detail_article,
    @StringRes val effectiveDateRes: Int = R.string.analysis_legal_reference_detail_effective_date,
    @StringRes val fullTextRes: Int = R.string.analysis_legal_reference_detail_full_text,
    @StringRes val issuerRes: Int = R.string.analysis_legal_reference_detail_issuer_value,
    @StringRes val documentTypeRes: Int = R.string.analysis_legal_reference_detail_document_type_value,
    @StringRes val statusRes: Int = R.string.analysis_legal_reference_detail_status_active,
    @StringRes val relatedRiskTitleRes: Int = R.string.analysis_risk_findings_penalty_title,
    @StringRes val relatedRiskSubtitleRes: Int = R.string.analysis_legal_reference_detail_related_risk_subtitle
)
