package com.example.lagallens.presentation.feature.analysis.legalreferences.viewmodel

import com.example.lagallens.R
import com.example.lagallens.presentation.feature.analysis.legalreferences.contract.AnalysisLegalReferencesUiEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisLegalReferencesViewModelTest {
    @Test
    fun queryFiltersReferencesByLegalDocumentContent() {
        val viewModel = AnalysisLegalReferencesViewModel()

        viewModel.onEvent(AnalysisLegalReferencesUiEvent.QueryChanged("dân sự"))

        assertEquals(1, viewModel.uiState.value.references.size)
        assertEquals(
            R.string.analysis_legal_references_civil_code_title,
            viewModel.uiState.value.references.single().titleRes
        )
    }

    @Test
    fun screenOpenedPreservesAnalysisDocumentContext() {
        val viewModel = AnalysisLegalReferencesViewModel()

        viewModel.onEvent(
            AnalysisLegalReferencesUiEvent.ScreenOpened(
                documentName = "hop-dong-lao-dong.pdf",
                documentMetadata = "3 trang · 1,2 MB"
            )
        )

        assertEquals("hop-dong-lao-dong.pdf", viewModel.uiState.value.documentName)
        assertEquals("3 trang · 1,2 MB", viewModel.uiState.value.documentMetadata)
    }
}
