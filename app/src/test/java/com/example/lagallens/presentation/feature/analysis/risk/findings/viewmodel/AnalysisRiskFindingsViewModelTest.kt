package com.example.lagallens.presentation.feature.analysis.risk.findings.viewmodel

import com.example.lagallens.presentation.feature.analysis.risk.findings.contract.AnalysisRiskFindingsUiEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisRiskFindingsViewModelTest {
    @Test
    fun screenOpenedPreservesDocumentContext() {
        val viewModel = AnalysisRiskFindingsViewModel()

        viewModel.onEvent(
            AnalysisRiskFindingsUiEvent.ScreenOpened(
                documentName = "HÃ¡Â»Â£p Ã„â€˜Ã¡Â»â€œng thuÃƒÂª vÃ„Æ’n phÃƒÂ²ng",
                documentMetadata = "DÃ¡Â»â€¹ch vÃ¡Â»Â¥ Ã‚Â· 12 trang"
            )
        )

        assertEquals("HÃ¡Â»Â£p Ã„â€˜Ã¡Â»â€œng thuÃƒÂª vÃ„Æ’n phÃƒÂ²ng", viewModel.uiState.value.documentName)
        assertEquals("DÃ¡Â»â€¹ch vÃ¡Â»Â¥ Ã‚Â· 12 trang", viewModel.uiState.value.documentMetadata)
    }
    @Test
    fun findingsFollowTheSelectedResult() {
        val viewModel = AnalysisRiskFindingsViewModel()
        val first = com.example.lagallens.domain.model.analysis.AnalysisIdentity("r1", "o1", "c1", "d1", listOf("penalty-cap"), snapshot = riskFixture("penalty-cap", "penalty", "HIGH"))
        val second = com.example.lagallens.domain.model.analysis.AnalysisIdentity("r2", "o2", "c2", "d2", listOf("termination-notice"), snapshot = riskFixture("termination-notice", "termination", "MEDIUM"))
        viewModel.setResult(first, detail(first), isUnlocked = true)
        assertEquals(listOf("penalty-cap"), viewModel.uiState.value.findings.map { it.id })
        viewModel.setResult(second, detail(second), isUnlocked = true)
        assertEquals(listOf("termination-notice"), viewModel.uiState.value.findings.map { it.id })
    }
    private fun detail(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity) =
        com.example.lagallens.domain.model.analysis.AnalysisDetail(emptyList(), emptyList(), emptyList(), emptyList(), identity.findingIds.map {
            com.example.lagallens.domain.model.analysis.AnalysisFinding(it, com.example.lagallens.domain.model.analysis.AnalysisSeverity.HIGH, "c", "t", "d", "x")
        })

    private fun riskFixture(id: String, name: String, severity: String): Map<String, String> {
        val keys = listOf("clause", "title", "description", "consequence").map { "analysis_risk_findings_${name}_$it" }
        val row = keys.joinToString(",") { "\"$it\"" }
        val out = mutableMapOf("layout:findings" to "{\"$id\":[$row]}", "risk:$id:severity" to severity)
        keys.forEach { out["vi:$it"] = "demo"; out["en:$it"] = "demo" }
        return out
    }
}
