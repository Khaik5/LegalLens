package com.example.lagallens.presentation.feature.ocr.viewmodel

import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewEvent
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrReviewViewModelTest {
    @Test
    fun restoreOriginalRestoresTheOriginalOcrText() {
        val viewModel = OcrReviewViewModel()

        viewModel.onEvent(OcrReviewEvent.RestoreOriginalClicked)

        assertEquals(OcrReviewSample.originalText, viewModel.uiState.value.text)
        assertTrue(viewModel.uiState.value.isOriginalText)
    }

    @Test
    fun editingTextMarksTheOcrTextAsChanged() {
        val viewModel = OcrReviewViewModel()

        viewModel.onEvent(OcrReviewEvent.TextChanged("Nội dung đã chỉnh sửa"))

        assertEquals("Nội dung đã chỉnh sửa", viewModel.uiState.value.text)
        assertFalse(viewModel.uiState.value.isOriginalText)
    }
}
