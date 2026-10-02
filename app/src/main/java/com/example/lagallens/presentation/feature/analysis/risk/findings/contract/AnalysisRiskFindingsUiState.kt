package com.example.lagallens.presentation.feature.analysis.risk.findings.contract

import com.example.lagallens.R
import com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskFilter
import com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskFinding
import com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskSeverity

data class AnalysisRiskFindingsUiState(
    val selectedFilter: AnalysisRiskFilter = AnalysisRiskFilter.ALL,
    val findings: List<AnalysisRiskFinding> = allFindings
) {
    companion object {
        val allFindings = listOf(
            AnalysisRiskFinding(
                id = "penalty-cap",
                severity = AnalysisRiskSeverity.HIGH,
                clauseRes = R.string.analysis_risk_findings_penalty_clause,
                titleRes = R.string.analysis_risk_findings_penalty_title,
                descriptionRes = R.string.analysis_risk_findings_penalty_description,
                consequenceRes = R.string.analysis_risk_findings_penalty_consequence
            ),
            AnalysisRiskFinding(
                id = "termination-notice",
                severity = AnalysisRiskSeverity.MEDIUM,
                clauseRes = R.string.analysis_risk_findings_termination_clause,
                titleRes = R.string.analysis_risk_findings_termination_title,
                descriptionRes = R.string.analysis_risk_findings_termination_description,
                consequenceRes = R.string.analysis_risk_findings_termination_consequence
            ),
            AnalysisRiskFinding(
                id = "dispute-resolution",
                severity = AnalysisRiskSeverity.LOW,
                clauseRes = R.string.analysis_risk_findings_dispute_clause,
                titleRes = R.string.analysis_risk_findings_dispute_title,
                descriptionRes = R.string.analysis_risk_findings_dispute_description,
                consequenceRes = R.string.analysis_risk_findings_dispute_consequence
            )
        )
    }
}
