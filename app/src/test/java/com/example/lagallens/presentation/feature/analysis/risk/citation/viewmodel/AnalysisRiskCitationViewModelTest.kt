package com.example.lagallens.presentation.feature.analysis.risk.citation.viewmodel

import com.example.lagallens.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisRiskCitationViewModelTest {
    @Test
    fun initialStateUsesTheApprovedRiskAndLegalReference() {
        val state = AnalysisRiskCitationViewModel().uiState.value

        assertEquals(R.string.analysis_risk_findings_penalty_title, state.findingTitleRes)
        assertEquals(R.string.analysis_risk_citation_legal_reference, state.legalReferenceRes)
    }
}
