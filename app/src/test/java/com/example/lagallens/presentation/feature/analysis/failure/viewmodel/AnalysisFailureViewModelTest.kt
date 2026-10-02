package com.example.lagallens.presentation.feature.analysis.failure.viewmodel

import com.example.lagallens.presentation.feature.analysis.failure.contract.AnalysisFailureUiEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisFailureViewModelTest {
    @Test
    fun screenOpenedPreservesDocumentMetadataForRetry() {
        val viewModel = AnalysisFailureViewModel()

        viewModel.onEvent(
            AnalysisFailureUiEvent.ScreenOpened(
                documentName = "hop-dong-lao-dong.pdf",
                documentMetadata = "3 trang · 1,2 MB"
            )
        )

        assertEquals("hop-dong-lao-dong.pdf", viewModel.uiState.value.documentName)
        assertEquals("3 trang · 1,2 MB", viewModel.uiState.value.documentMetadata)
    }
}
