package com.example.lagallens.presentation.feature.analysis.risk

import com.example.lagallens.domain.model.analysis.AnalysisDetail
import com.example.lagallens.domain.model.analysis.AnalysisFinding
import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import com.example.lagallens.domain.model.analysis.AnalysisSeverity
import com.example.lagallens.presentation.feature.analysis.risk.findings.viewmodel.AnalysisRiskFindingsViewModel
import com.example.lagallens.presentation.feature.analysis.risk.viewmodel.AnalysisRiskOverviewViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

/** The Risk Overview and the findings list must agree on what is visible and on the total. */
class AnalysisRiskCountsTest {
    private val identity = AnalysisIdentity("result", "operation", "contract", "document")

    private fun finding(id: String, severity: AnalysisSeverity) = AnalysisFinding(id, severity, "Điều 1", "Tiêu đề $id", "Mô tả", "Hệ quả")

    /** Eight HIGH findings and one LOW, as a finished analysis returns them. */
    private fun fullDetail() = detail((1..8).map { finding("f$it", AnalysisSeverity.HIGH) } + finding("f9", AnalysisSeverity.LOW))

    /** The server sends the first four of nine (40% rounded up); the mapper pads the rest with empty placeholders typed LOW. */
    private fun lockedDetail() = detail(
        (1..4).map { finding("f$it", AnalysisSeverity.HIGH) } + (0 until 5).map { finding("locked-finding-$it", AnalysisSeverity.LOW) }
    )

    private fun detail(findings: List<AnalysisFinding>) = AnalysisDetail(emptyList(), emptyList(), emptyList(), emptyList(), findings)

    @Test fun aLockedResultCountsOnlyTheFreePreviewButReportsTheRealTotal() {
        val viewModel = AnalysisRiskOverviewViewModel()

        viewModel.setResult(identity, lockedDetail(), isUnlocked = false)

        val state = viewModel.uiState.value
        assertEquals(9, state.totalRiskCount)
        assertEquals(5, state.lockedRiskCount)
        assertEquals(4, state.highRiskCount)
        assertEquals(0, state.mediumRiskCount)
        assertEquals(0, state.lowRiskCount)
    }

    @Test fun anUnlockedResultCountsEveryFinding() {
        val viewModel = AnalysisRiskOverviewViewModel()

        viewModel.setResult(identity, fullDetail(), isUnlocked = true)

        val state = viewModel.uiState.value
        assertEquals(9, state.totalRiskCount)
        assertEquals(0, state.lockedRiskCount)
        assertEquals(8, state.highRiskCount)
        assertEquals(1, state.lowRiskCount)
    }

    @Test fun theFindingsListShowsTheSameShareAsTheOverviewCounts() {
        val overview = AnalysisRiskOverviewViewModel().apply { setResult(identity, lockedDetail(), isUnlocked = false) }
        val list = AnalysisRiskFindingsViewModel().apply { setResult(identity, lockedDetail(), isUnlocked = false) }

        val visibleInOverview = overview.uiState.value.let { it.highRiskCount + it.mediumRiskCount + it.lowRiskCount }
        assertEquals(visibleInOverview, list.uiState.value.findings.size)
        assertEquals(overview.uiState.value.lockedRiskCount, list.uiState.value.lockedCount)
    }

    @Test fun filterTabsCountTheVisibleFindingsPerLevel() {
        val viewModel = AnalysisRiskFindingsViewModel()

        viewModel.setResult(identity, fullDetail(), isUnlocked = true)

        val state = viewModel.uiState.value
        assertEquals(9, state.allCount)
        assertEquals(8, state.highCount)
        assertEquals(0, state.mediumCount)
        assertEquals(1, state.lowCount)
    }

    @Test fun filterTabsOfALockedResultCountOnlyWhatIsShown() {
        val viewModel = AnalysisRiskFindingsViewModel()

        viewModel.setResult(identity, lockedDetail(), isUnlocked = false)

        val state = viewModel.uiState.value
        assertEquals(4, state.allCount)
        assertEquals(4, state.highCount)
        assertEquals(0, state.lowCount)
        assertEquals(5, state.lockedCount)
    }
}
