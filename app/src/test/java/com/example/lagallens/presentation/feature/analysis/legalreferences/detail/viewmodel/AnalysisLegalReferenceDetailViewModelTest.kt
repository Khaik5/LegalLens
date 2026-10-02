package com.example.lagallens.presentation.feature.analysis.legalreferences.detail.viewmodel

import com.example.lagallens.presentation.feature.analysis.legalreferences.detail.contract.AnalysisLegalReferenceDetailUiEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisLegalReferenceDetailViewModelTest {
    @Test
    fun screenOpenedPreservesReferenceAndDocumentContext() {
        val viewModel = AnalysisLegalReferenceDetailViewModel()

        viewModel.onEvent(
            AnalysisLegalReferenceDetailUiEvent.ScreenOpened(
                referenceId = "commercial-law-2005-article-301",
                documentName = "hop-dong-lao-dong.pdf",
                documentMetadata = "3 trang · 1,2 MB"
            )
        )

        assertEquals("commercial-law-2005-article-301", viewModel.uiState.value.referenceId)
        assertEquals("hop-dong-lao-dong.pdf", viewModel.uiState.value.documentName)
        assertEquals("3 trang · 1,2 MB", viewModel.uiState.value.documentMetadata)
    }
}
