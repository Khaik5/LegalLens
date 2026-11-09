package com.example.lagallens.presentation.feature.ocr.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewEvent
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewWarning
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OcrReviewViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun reviewShowsOnlyWarningsFromTheOcrResult() {
        val viewModel = OcrReviewViewModel()
        assertEquals(emptyList<OcrReviewWarning>(), viewModel.uiState.value.warnings)

        val warning = OcrReviewWarning("Section", "60%", "value", "source")
        viewModel.onEvent(OcrReviewEvent.ScreenOpened("doc", null, "text", listOf(warning)))

        assertEquals(listOf(warning), viewModel.uiState.value.warnings)
    }

    @Test
    fun confirmClickDoesNotMutateTheDisplayedWarnings() {
        val viewModel = OcrReviewViewModel()
        val warning = OcrReviewWarning("Section", "60%", "value", "source")
        viewModel.onEvent(OcrReviewEvent.ScreenOpened("doc", null, "text", listOf(warning)))

        viewModel.onEvent(OcrReviewEvent.ConfirmClicked)

        assertEquals(listOf(warning), viewModel.uiState.value.warnings)
    }

    @Test
    fun reviewKeepsDocumentIdentityAndEditableContent() {
        val viewModel = OcrReviewViewModel()
        val editedContent = "ĐIỀU 1. Nội dung người dùng đã sửa"

        viewModel.onEvent(OcrReviewEvent.ScreenOpened("doc-9", "contract-4", "nội dung gốc"))
        viewModel.onEvent(OcrReviewEvent.DocumentTextChanged(editedContent))

        assertEquals("doc-9", viewModel.uiState.value.documentId)
        assertEquals("contract-4", viewModel.uiState.value.contractId)
        assertEquals(editedContent, viewModel.uiState.value.documentText)
    }
}
