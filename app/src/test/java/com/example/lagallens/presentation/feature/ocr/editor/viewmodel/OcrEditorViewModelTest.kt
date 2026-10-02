package com.example.lagallens.presentation.feature.ocr.editor.viewmodel

import com.example.lagallens.presentation.feature.ocr.editor.contract.OcrEditorUiEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrEditorViewModelTest {
    @Test
    fun restoreOriginalRestoresTheOcrResultText() {
        val viewModel = OcrEditorViewModel()
        val originalText = "Nội dung OCR từ màn kết quả"

        viewModel.onEvent(OcrEditorUiEvent.Initialize(originalText))
        viewModel.onEvent(OcrEditorUiEvent.TextChanged("Nội dung đã chỉnh sửa"))
        viewModel.onEvent(OcrEditorUiEvent.RestoreOriginalClicked)

        assertEquals(originalText, viewModel.uiState.value.text)
        assertTrue(viewModel.uiState.value.isOriginalText)
    }
}
