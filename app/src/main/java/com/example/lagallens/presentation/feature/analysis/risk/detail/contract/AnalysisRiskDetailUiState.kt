package com.example.lagallens.presentation.feature.analysis.risk.detail.contract

import androidx.annotation.StringRes
import androidx.annotation.DrawableRes
import androidx.annotation.ColorRes
import com.example.lagallens.R

data class AnalysisRiskDetailUiState(
    val isDetailedExample: Boolean = false,
    val clauseReference: String = "",
    @StringRes val severityRes: Int = R.string.analysis_risk_detail_severity_high,
    @DrawableRes val severityBackgroundRes: Int = R.drawable.bg_analysis_risk_findings_high_badge,
    @ColorRes val severityColorRes: Int = R.color.contract_risk_high,
    val title: String = "",
    val attentionReason: String = "",
    val clauseExcerpt: String = "",
    val legalExplanation: String? = null,
    @StringRes val legalReferenceTitleRes: Int = R.string.analysis_risk_detail_legal_reference_title,
    @StringRes val legalReferenceSubtitleRes: Int = R.string.analysis_risk_detail_legal_reference_subtitle,
    val recommendation: String? = null
)
