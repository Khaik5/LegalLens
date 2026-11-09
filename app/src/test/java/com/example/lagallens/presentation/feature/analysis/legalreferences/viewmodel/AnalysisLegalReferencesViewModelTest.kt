package com.example.lagallens.presentation.feature.analysis.legalreferences.viewmodel

import com.example.lagallens.presentation.feature.analysis.legalreferences.contract.AnalysisLegalReferencesUiEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalysisLegalReferencesViewModelTest {
    @Test
    fun demoPreviewKeepsLockedReferencesHiddenFromSearchUntilUnlocked() {
        val samples = com.example.lagallens.presentation.feature.analysis.legalreferences.model.DemoLegalReferences.items
        val viewModel = AnalysisLegalReferencesViewModel()
        viewModel.setDemoReferences(samples, false)
        assertEquals(listOf(samples.first().id), viewModel.uiState.value.references.map { it.id })
        assertEquals(2, viewModel.uiState.value.lockedCount)
        viewModel.onEvent(AnalysisLegalReferencesUiEvent.QueryChanged("dân sự"))
        assertTrue(viewModel.uiState.value.references.isEmpty())
        viewModel.setDemoReferences(samples, true)
        assertEquals(listOf(samples[1].id), viewModel.uiState.value.references.map { it.id })
        assertEquals(0, viewModel.uiState.value.lockedCount)
        assertFalse(com.example.lagallens.presentation.feature.analysis.legalreferences.model.DemoLegalReferences.canView(samples[1].id, false))
        assertTrue(com.example.lagallens.presentation.feature.analysis.legalreferences.model.DemoLegalReferences.canView(samples[1].id, true))
    }
    @Test
    fun queryDoesNotPresentUnverifiedReferences() {
        val viewModel = AnalysisLegalReferencesViewModel()

        viewModel.onEvent(AnalysisLegalReferencesUiEvent.QueryChanged("dân sự"))

        assertEquals(0, viewModel.uiState.value.references.size)
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
