package com.example.lagallens.presentation.feature.analysis.risk.detail.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.presentation.feature.analysis.risk.detail.contract.AnalysisRiskDetailUiEffect
import com.example.lagallens.presentation.feature.analysis.risk.detail.contract.AnalysisRiskDetailUiEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AnalysisRiskDetailViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    @Test fun unverifiedCitationShowsUnavailableInsteadOfOpeningSampleLaw() = runTest {
        val vm = AnalysisRiskDetailViewModel()
        val effect = async { vm.uiEffect.first() }
        yield()
        vm.onEvent(AnalysisRiskDetailUiEvent.LegalReferenceClicked)
        assertEquals(AnalysisRiskDetailUiEffect.CitationUnavailable, effect.await())
    }
}
