package com.example.lagallens.presentation.feature.ocr.failure.viewmodel

import com.example.lagallens.presentation.feature.ocr.failure.contract.OcrFailureUiEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class OcrFailureViewModelTest {
    @Test
    fun screenOpenedPreservesDocumentMetadataForRetry() {
        val viewModel = OcrFailureViewModel()

        viewModel.onEvent(
            OcrFailureUiEvent.ScreenOpened(
                documentName = "hop-dong-lao-dong.pdf",
                documentDetails = "3 trang"
            )
        )

        assertEquals("hop-dong-lao-dong.pdf", viewModel.uiState.value.documentName)
        assertEquals("3 trang", viewModel.uiState.value.documentDetails)
    }
}
