package com.example.lagallens.presentation.feature.analysis.start.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.analysis.start.contract.AnalysisStartUiEffect
import com.example.lagallens.presentation.feature.analysis.start.contract.AnalysisStartUiEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AnalysisStartViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test fun startingOpensProcessingWhenAnalysisIsAvailable() = runTest {
        val viewModel = AnalysisStartViewModel(isAnalysisAvailable = true)
        viewModel.onEvent(AnalysisStartUiEvent.ScreenOpened("HĐ lao động", "PDF · 2 trang"))

        viewModel.onEvent(AnalysisStartUiEvent.StartAnalysisClicked)

        assertEquals(
            AnalysisStartUiEffect.NavigateToAnalysisProcessing("HĐ lao động", "PDF · 2 trang"),
            viewModel.uiEffect.first()
        )
    }

    @Test fun startingExplainsWhenAnalysisIsUnavailable() = runTest {
        val viewModel = AnalysisStartViewModel(isAnalysisAvailable = false)
        viewModel.onEvent(AnalysisStartUiEvent.ScreenOpened("HĐ lao động", "PDF · 2 trang"))

        viewModel.onEvent(AnalysisStartUiEvent.StartAnalysisClicked)

        assertEquals(AnalysisStartUiEffect.ShowMessage(R.string.analysis_ai_unavailable), viewModel.uiEffect.first())
    }
}
