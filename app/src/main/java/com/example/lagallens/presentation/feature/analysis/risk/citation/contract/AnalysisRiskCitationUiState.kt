package com.example.lagallens.presentation.feature.analysis.risk.citation.contract

import androidx.annotation.StringRes
import com.example.lagallens.R

data class AnalysisRiskCitationUiState(
    @StringRes val noticeRes: Int = R.string.analysis_risk_citation_notice,
    @StringRes val findingLabelRes: Int = R.string.analysis_risk_citation_finding_label,
    @StringRes val findingTitleRes: Int = R.string.analysis_risk_findings_penalty_title,
    @StringRes val findingSourceRes: Int = R.string.analysis_risk_citation_finding_source,
    @StringRes val citationHeadingRes: Int = R.string.analysis_risk_citation_heading,
    @StringRes val legalReferenceRes: Int = R.string.analysis_risk_citation_legal_reference,
    @StringRes val citationBodyRes: Int = R.string.analysis_risk_citation_body,
    @StringRes val citationHighlightRes: Int = R.string.analysis_risk_citation_highlight
)
