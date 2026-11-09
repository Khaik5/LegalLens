package com.example.lagallens.presentation.feature.analysis

import com.example.lagallens.domain.model.analysis.*
import com.example.lagallens.presentation.feature.analysis.keyfacts.viewmodel.AnalysisKeyFactsViewModel
import com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsListItem
import com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsUiEvent
import com.example.lagallens.presentation.feature.analysis.obligations.viewmodel.AnalysisObligationsViewModel
import com.example.lagallens.presentation.feature.analysis.parties.viewmodel.AnalysisPartiesViewModel
import com.example.lagallens.presentation.feature.analysis.risk.findings.contract.AnalysisRiskFindingsUiEvent
import com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskFilter
import com.example.lagallens.presentation.feature.analysis.risk.findings.viewmodel.AnalysisRiskFindingsViewModel
import com.example.lagallens.presentation.feature.analysis.summary.viewmodel.AnalysisSummaryViewModel
import org.junit.Assert.*
import org.junit.Test

class AnalysisPreviewScreensTest {
    private val identity = AnalysisIdentity("result", "operation", "contract", "document", listOf("risk1", "risk2", "risk3"))
    private val detail = AnalysisDetail(
        (1..5).map { AnalysisSummaryEntry("summary$it", "title$it", "description") },
        (1..6).map { AnalysisKeyFactEntry("fact$it", "label", "reference", "value", "description") },
        (1..2).map { AnalysisPartyEntry("party$it", "title", "role", emptyList()) },
        listOf(AnalysisObligationEntry.Section("a", "A", AnalysisObligationTab.RIGHTS)) +
            (1..3).map { AnalysisObligationEntry.Item("rights$it", "title", "clause", "description") } +
            listOf(AnalysisObligationEntry.Section("b", "B", AnalysisObligationTab.DUTIES)) +
            (1..2).map { AnalysisObligationEntry.Item("duties$it", "title", "clause", "description") },
        listOf(AnalysisFinding("risk1", AnalysisSeverity.HIGH, "c", "t", "d", "x"), AnalysisFinding("risk2", AnalysisSeverity.MEDIUM, "c", "t", "d", "x"), AnalysisFinding("risk3", AnalysisSeverity.LOW, "c", "t", "d", "x"))
    )

    @Test fun previewCountHandlesRoundingAndEmptyLists() {
        assertEquals(0, AnalysisPreview.visibleCount(0, false))
        assertEquals(1, AnalysisPreview.visibleCount(1, false))
        assertEquals(1, AnalysisPreview.visibleCount(2, false))
        assertEquals(3, AnalysisPreview.visibleCount(8, false))
        assertEquals(8, AnalysisPreview.visibleCount(8, true))
    }

    @Test fun listsShowOnlyFreeEntriesThenAllAfterUnlock() {
        val summary = AnalysisSummaryViewModel(); val facts = AnalysisKeyFactsViewModel(); val parties = AnalysisPartiesViewModel()
        summary.setResult(identity, detail, false); facts.setResult(identity, detail, false); parties.setResult(identity, detail, false)
        assertEquals(2, summary.uiState.value.sections.size); assertEquals(3, summary.uiState.value.lockedCount)
        assertEquals(2, facts.uiState.value.facts.size); assertEquals(4, facts.uiState.value.lockedCount)
        assertEquals(1, parties.uiState.value.parties.size); assertEquals(1, parties.uiState.value.lockedCount)
        summary.setResult(identity, detail, true); facts.setResult(identity, detail, true); parties.setResult(identity, detail, true)
        assertEquals(5, summary.uiState.value.sections.size); assertEquals(6, facts.uiState.value.facts.size); assertEquals(2, parties.uiState.value.parties.size)
    }

    @Test fun obligationTabsCalculatePreviewSeparately() {
        val vm = AnalysisObligationsViewModel(); vm.setResult(identity, detail, false)
        assertEquals(listOf("rights1"), vm.uiState.value.items.filterIsInstance<AnalysisObligationsListItem.Obligation>().map { it.obligation.id })
        assertEquals(2, vm.uiState.value.lockedCount)
        vm.onEvent(AnalysisObligationsUiEvent.TabSelected(AnalysisObligationTab.DUTIES))
        assertEquals(listOf("duties1"), vm.uiState.value.items.filterIsInstance<AnalysisObligationsListItem.Obligation>().map { it.obligation.id })
        assertEquals(1, vm.uiState.value.lockedCount)
        vm.setResult(identity, detail, true)
        assertEquals(2, vm.uiState.value.items.filterIsInstance<AnalysisObligationsListItem.Obligation>().size)
        assertEquals(0, vm.uiState.value.lockedCount)
    }

    @Test fun riskFilterCannotRevealLockedEntries() {
        val vm = AnalysisRiskFindingsViewModel(); vm.setResult(identity, detail, false)
        assertEquals(listOf("risk1"), vm.uiState.value.findings.map { it.id })
        vm.onEvent(AnalysisRiskFindingsUiEvent.FilterClicked(AnalysisRiskFilter.MEDIUM))
        assertTrue(vm.uiState.value.findings.isEmpty())
        vm.setResult(identity, detail, true)
        assertEquals(listOf("risk2"), vm.uiState.value.findings.map { it.id })
    }
}
