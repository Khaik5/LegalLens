package com.example.lagallens.presentation.feature.analysis.risk.findings.model

enum class AnalysisRiskFilter(val severity: AnalysisRiskSeverity?) {
    ALL(null),
    HIGH(AnalysisRiskSeverity.HIGH),
    MEDIUM(AnalysisRiskSeverity.MEDIUM),
    LOW(AnalysisRiskSeverity.LOW)
}
