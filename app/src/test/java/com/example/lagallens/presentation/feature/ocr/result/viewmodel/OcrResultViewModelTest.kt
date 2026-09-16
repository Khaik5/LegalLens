package com.example.lagallens.presentation.feature.ocr.result.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.presentation.feature.ocr.result.contract.OcrResultUiEvent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OcrResultViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun documentUpdateReplacesTheDisplayedOcrText() {
        val viewModel = OcrResultViewModel()
        val editedText = "Nội dung OCR đã được người dùng chỉnh sửa"

        viewModel.onEvent(OcrResultUiEvent.DocumentUpdated(editedText))

        assertEquals(editedText, viewModel.uiState.value.documentText)
    }

    @Test
    fun reviewConfirmationIsKeptForTheCurrentSession() {
        val viewModel = OcrResultViewModel()

        viewModel.onEvent(OcrResultUiEvent.ReviewConfirmed)

        assertEquals(true, viewModel.uiState.value.isReviewConfirmed)
    }
}
