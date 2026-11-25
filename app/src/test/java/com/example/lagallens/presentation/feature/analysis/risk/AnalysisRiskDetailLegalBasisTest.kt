package com.example.lagallens.presentation.feature.analysis.risk

import com.example.lagallens.domain.model.analysis.AnalysisCitation
import com.example.lagallens.domain.model.analysis.AnalysisDetail
import com.example.lagallens.domain.model.analysis.AnalysisFinding
import com.example.lagallens.domain.model.analysis.AnalysisIdentity
import com.example.lagallens.domain.model.analysis.AnalysisSeverity
import com.example.lagallens.presentation.feature.analysis.risk.detail.viewmodel.AnalysisRiskDetailViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisRiskDetailLegalBasisTest {
    private fun citation(id: String, excerpt: String = "Nội dung điều luật") =
        AnalysisCitation(id, 521, "Bộ luật Lao động", "26", "Tiền lương thử việc", null, excerpt, null, null)

    private fun detail(finding: AnalysisFinding, vararg citations: AnalysisCitation) = AnalysisDetail(
        emptyList(), emptyList(), emptyList(), emptyList(), listOf(finding), citations.toList()
    )

    private fun finding(citationIds: List<String>, verified: Boolean?, excerpt: String? = null) = AnalysisFinding(
        "f1", AnalysisSeverity.HIGH, "Điều 4", "Lương thử việc", "Mô tả", "Hệ quả",
        isLegalBasisVerified = verified, clauseExcerpt = excerpt, citationIds = citationIds
    )

    private fun load(detail: AnalysisDetail): AnalysisRiskDetailViewModel {
        val viewModel = AnalysisRiskDetailViewModel()
        assertTrue(viewModel.loadFinding("f1", AnalysisIdentity("r", "o", "c", "d", listOf("f1")), detail))
        return viewModel
    }

    @Test fun theFindingShowsTheArticlesItCites() {
        val state = load(detail(finding(listOf("c1", "c2"), true), citation("c1"), citation("c2"))).uiState.value

        assertEquals(listOf("c1", "c2"), state.legalBasis.map { it.id })
        assertEquals("26", state.legalBasis.first().articleNumber)
        assertFalse(state.isLegalBasisUnverified)
    }

    @Test fun aCitationThatIsStillLockedIsNotShown() {
        val state = load(detail(finding(listOf("c1", "c2"), true), citation("c1"), citation("c2", excerpt = ""))).uiState.value

        assertEquals(listOf("c1"), state.legalBasis.map { it.id })
    }

    @Test fun aFindingWithoutALegalSourceIsMarkedUnverified() {
        val state = load(detail(finding(emptyList(), false), citation("c1"))).uiState.value

        assertTrue(state.legalBasis.isEmpty())
        assertTrue(state.isLegalBasisUnverified)
    }

    @Test fun theOriginalClauseIsShownWhenTheBackendFoundIt() {
        val state = load(detail(finding(emptyList(), null, excerpt = "Thử việc 60 ngày"))).uiState.value

        assertEquals("Thử việc 60 ngày", state.originalExcerpt)
    }
}
