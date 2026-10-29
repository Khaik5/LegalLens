package com.example.lagallens.presentation.feature.analysis.risk.detail.contract

import androidx.annotation.StringRes
import androidx.annotation.DrawableRes
import androidx.annotation.ColorRes
import com.example.lagallens.R

data class AnalysisRiskDetailUiState(
    val isDetailedExample: Boolean = true,
    @StringRes val clauseReferenceRes: Int = R.string.analysis_risk_detail_clause_reference,
    @StringRes val severityRes: Int = R.string.analysis_risk_detail_severity_high,
    @DrawableRes val severityBackgroundRes: Int = R.drawable.bg_analysis_risk_findings_high_badge,
    @ColorRes val severityColorRes: Int = R.color.contract_risk_high,
    @StringRes val titleRes: Int = R.string.analysis_risk_findings_penalty_title,
    @StringRes val attentionReasonRes: Int = R.string.analysis_risk_detail_attention_reason,
    @StringRes val clauseExcerptRes: Int = R.string.analysis_risk_detail_clause_excerpt,
    @StringRes val legalExplanationRes: Int = R.string.analysis_risk_detail_legal_explanation,
    @StringRes val legalReferenceTitleRes: Int = R.string.analysis_risk_detail_legal_reference_title,
    @StringRes val legalReferenceSubtitleRes: Int = R.string.analysis_risk_detail_legal_reference_subtitle,
    @StringRes val recommendationRes: Int = R.string.analysis_risk_detail_recommendation
)
