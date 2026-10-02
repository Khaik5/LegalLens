package com.example.lagallens.presentation.feature.analysis.risk.detail.contract

import androidx.annotation.StringRes
import com.example.lagallens.R

data class AnalysisRiskDetailUiState(
    @StringRes val severityRes: Int = R.string.analysis_risk_detail_severity_high,
    @StringRes val titleRes: Int = R.string.analysis_risk_findings_penalty_title,
    @StringRes val attentionReasonRes: Int = R.string.analysis_risk_detail_attention_reason,
    @StringRes val clauseExcerptRes: Int = R.string.analysis_risk_detail_clause_excerpt,
    @StringRes val legalExplanationRes: Int = R.string.analysis_risk_detail_legal_explanation,
    @StringRes val legalReferenceTitleRes: Int = R.string.analysis_risk_detail_legal_reference_title,
    @StringRes val legalReferenceSubtitleRes: Int = R.string.analysis_risk_detail_legal_reference_subtitle,
    @StringRes val recommendationRes: Int = R.string.analysis_risk_detail_recommendation
)
