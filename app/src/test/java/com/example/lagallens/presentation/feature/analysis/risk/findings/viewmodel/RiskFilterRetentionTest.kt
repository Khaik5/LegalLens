package com.example.lagallens.presentation.feature.analysis.risk.findings.viewmodel

import com.example.lagallens.domain.model.analysis.*
import com.example.lagallens.presentation.feature.analysis.risk.findings.contract.AnalysisRiskFindingsUiEvent
import com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskFilter
import org.junit.Assert.*
import org.junit.Test

class RiskFilterRetentionTest {
    @Test fun refreshKeepsFilterAndContextButNewResultResetsIt() {
        val vm = AnalysisRiskFindingsViewModel()
        val identity = AnalysisIdentity("result", "op", "contract", "doc")
        val detail = AnalysisDetail(emptyList(), emptyList(), emptyList(), emptyList(), listOf(
            AnalysisFinding("high", AnalysisSeverity.HIGH, "1", "h", "d", "c"),
            AnalysisFinding("low", AnalysisSeverity.LOW, "2", "l", "d", "c")
        ))
        vm.onEvent(AnalysisRiskFindingsUiEvent.ScreenOpened("My document", "metadata"))
        vm.setResult(identity, detail, isUnlocked = true)
        vm.onEvent(AnalysisRiskFindingsUiEvent.FilterClicked(AnalysisRiskFilter.LOW))
        vm.setResult(identity, detail, isUnlocked = true)
        assertEquals(AnalysisRiskFilter.LOW, vm.uiState.value.selectedFilter)
        assertEquals(listOf("low"), vm.uiState.value.findings.map { it.id })
        assertEquals("My document", vm.uiState.value.documentName)
        vm.setResult(identity.copy(resultId = "another"), detail, isUnlocked = true)
        assertEquals(AnalysisRiskFilter.ALL, vm.uiState.value.selectedFilter)
        vm.clearResult()
        assertTrue(vm.uiState.value.findings.isEmpty())
    }
}
