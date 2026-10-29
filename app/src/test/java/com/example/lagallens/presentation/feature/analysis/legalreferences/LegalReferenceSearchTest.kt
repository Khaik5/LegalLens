package com.example.lagallens.presentation.feature.analysis.legalreferences

import com.example.lagallens.presentation.feature.analysis.legalreferences.contract.AnalysisLegalReferencesUiEvent
import com.example.lagallens.presentation.feature.analysis.legalreferences.viewmodel.AnalysisLegalReferencesViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class LegalReferenceSearchTest {
    @Test fun displayedDecreeNumberMatchesEquivalentSeparators() {
        val viewModel = AnalysisLegalReferencesViewModel()
        listOf("21/2021", "21-2021", "21  2021").forEach { query ->
            viewModel.onEvent(AnalysisLegalReferencesUiEvent.QueryChanged(query))
            assertEquals(listOf("decree-21-2021-article-15"), viewModel.uiState.value.references.map { it.id })
        }
    }

    @Test fun unknownQueryDoesNotMatchAndBlankRestoresAll() {
        val viewModel = AnalysisLegalReferencesViewModel()
        viewModel.onEvent(AnalysisLegalReferencesUiEvent.QueryChanged("not-a-reference"))
        assertEquals(0, viewModel.uiState.value.references.size)
        viewModel.onEvent(AnalysisLegalReferencesUiEvent.QueryChanged(""))
        assertEquals(3, viewModel.uiState.value.references.size)
    }

    @Test fun vietnameseAccentsAndArticleNumberStillMatch() {
        val viewModel = AnalysisLegalReferencesViewModel()
        listOf("LUẬT THƯƠNG MẠI", "điều 301").forEach { query ->
            viewModel.onEvent(AnalysisLegalReferencesUiEvent.QueryChanged(query))
            assertEquals(listOf("commercial-law-2005-article-301"), viewModel.uiState.value.references.map { it.id })
        }
    }
}
