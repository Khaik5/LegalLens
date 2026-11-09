package com.example.lagallens.presentation.feature.analysis.risk.findings.model

import androidx.annotation.DrawableRes
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.example.lagallens.R

data class AnalysisRiskFinding(
    val id: String,
    val severity: AnalysisRiskSeverity,
    val clause: String,
    val title: String,
    val description: String,
    val consequence: String
)

enum class AnalysisRiskSeverity(
    @StringRes val labelRes: Int,
    @DrawableRes val badgeBackgroundRes: Int,
    @ColorRes val labelColorRes: Int
) {
    HIGH(
        labelRes = R.string.analysis_risk_findings_high,
        badgeBackgroundRes = R.drawable.bg_analysis_risk_findings_high_badge,
        labelColorRes = R.color.contract_risk_high
    ),
    MEDIUM(
        labelRes = R.string.analysis_risk_findings_medium,
        badgeBackgroundRes = R.drawable.bg_analysis_risk_findings_medium_badge,
        labelColorRes = R.color.contract_pending
    ),
    LOW(
        labelRes = R.string.analysis_risk_findings_low,
        badgeBackgroundRes = R.drawable.bg_analysis_risk_findings_low_badge,
        labelColorRes = R.color.contract_complete
    )
}

internal fun com.example.lagallens.domain.model.analysis.AnalysisFinding.toRiskFinding() = AnalysisRiskFinding(
    id, AnalysisRiskSeverity.valueOf(severity.name), clause, title, description, consequence
)
