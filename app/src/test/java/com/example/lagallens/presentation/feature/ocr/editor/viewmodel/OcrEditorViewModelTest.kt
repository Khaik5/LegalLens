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

    @Test
    fun editingResumedAfterProcessDeathStartsFromTheSavedEdit() {
        val viewModel = OcrEditorViewModel()

        viewModel.onEvent(OcrEditorUiEvent.Initialize("Bản gốc", editedText = "Bản đã sửa dở"))

        assertEquals("Bản đã sửa dở", viewModel.uiState.value.text)
        assertEquals("Bản gốc", viewModel.uiState.value.originalText)
        assertTrue(!viewModel.uiState.value.isOriginalText)
        viewModel.onEvent(OcrEditorUiEvent.RestoreOriginalClicked)
        assertEquals("Bản gốc", viewModel.uiState.value.text)
    }

    @Test
    fun repeatedInitializeAfterRecreationKeepsTheUsersEdits() {
        val viewModel = OcrEditorViewModel()

        viewModel.onEvent(OcrEditorUiEvent.Initialize("Bản gốc"))
        viewModel.onEvent(OcrEditorUiEvent.TextChanged("Bản đã sửa"))
        // The Activity sends Initialize again after rotation; the ViewModel is the same instance.
        viewModel.onEvent(OcrEditorUiEvent.Initialize("Bản gốc"))

        assertEquals("Bản đã sửa", viewModel.uiState.value.text)
        assertEquals("Bản gốc", viewModel.uiState.value.originalText)
        assertTrue(!viewModel.uiState.value.isOriginalText)
    }
}
