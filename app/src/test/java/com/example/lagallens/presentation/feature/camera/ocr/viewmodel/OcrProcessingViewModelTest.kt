package com.example.lagallens.presentation.feature.camera.ocr.viewmodel

import com.example.lagallens.presentation.feature.camera.ocr.contract.OcrProcessingUiEffect
import com.example.lagallens.presentation.feature.camera.ocr.contract.OcrProcessingUiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OcrProcessingViewModelTest {
    @Test
    fun successfulProcessingCompletesEveryStageBeforeNavigatingToResult() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = OcrProcessingViewModel()
            val effects = mutableListOf<OcrProcessingUiEffect>()
            val effectCollection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffect.collect { effects.add(it) }
            }

            viewModel.onEvent(OcrProcessingUiEvent.ScreenOpened(shouldSimulateFailure = false))
            runCurrent()
            assertEquals(2, viewModel.uiState.value.activeStepId)
            assertTrue(effects.isEmpty())

            advanceTimeBy(900L)
            runCurrent()
            assertEquals(3, viewModel.uiState.value.activeStepId)
            assertTrue(effects.isEmpty())

            advanceTimeBy(900L)
            runCurrent()
            assertEquals(4, viewModel.uiState.value.activeStepId)
            assertTrue(effects.isEmpty())

            advanceTimeBy(900L)
            runCurrent()
            assertTrue(effects.single() is OcrProcessingUiEffect.NavigateToOcrResult)
            effectCollection.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun failedProcessingStopsAtImageAnalysisBeforeNavigatingToFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = OcrProcessingViewModel()
            val effects = mutableListOf<OcrProcessingUiEffect>()
            val effectCollection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffect.collect { effects.add(it) }
            }

            viewModel.onEvent(OcrProcessingUiEvent.ScreenOpened(shouldSimulateFailure = true))
            runCurrent()
            assertEquals(2, viewModel.uiState.value.activeStepId)

            advanceTimeBy(900L)
            runCurrent()
            assertEquals(2, viewModel.uiState.value.activeStepId)
            assertTrue(effects.single() is OcrProcessingUiEffect.NavigateToOcrFailure)
            effectCollection.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }
}
