package com.example.lagallens.presentation.feature.analysis.result.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.data.repository.FakeAnalysisRepository
import com.example.lagallens.domain.usecase.GetAnalysisOverviewUseCase
import com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultUiEvent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AnalysisResultViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun screenOpenedLoadsTheOverviewFromTheRepository() {
        val viewModel = AnalysisResultViewModel(
            GetAnalysisOverviewUseCase(FakeAnalysisRepository())
        )

        viewModel.onEvent(
            AnalysisResultUiEvent.ScreenOpened(
                documentName = "hop-dong.pdf",
                documentMetadata = "3 trang"
            )
        )

        val state = viewModel.uiState.value
        assertEquals("hop-dong.pdf", state.documentName)
        assertEquals(65, state.score)
        assertEquals(8, state.keyFactsCount)
        assertEquals(3, state.riskCount)
        assertEquals(12, state.referenceCount)
    }
}
