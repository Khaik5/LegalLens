package com.example.lagallens.presentation.feature.ocr.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewEvent
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewSample
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OcrReviewViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun reviewStateContainsTheTwoLocalWarnings() {
        val viewModel = OcrReviewViewModel()

        assertEquals(OcrReviewSample.warnings, viewModel.uiState.value.warnings)
        assertEquals(2, viewModel.uiState.value.warnings.size)
    }

    @Test
    fun confirmClickDoesNotMutateTheDisplayedWarnings() {
        val viewModel = OcrReviewViewModel()

        viewModel.onEvent(OcrReviewEvent.ConfirmClicked)

        assertEquals(OcrReviewSample.warnings, viewModel.uiState.value.warnings)
    }
}
