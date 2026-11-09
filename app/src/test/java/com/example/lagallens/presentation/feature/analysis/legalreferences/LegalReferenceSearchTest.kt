package com.example.lagallens.presentation.feature.analysis.legalreferences

import com.example.lagallens.presentation.feature.analysis.legalreferences.contract.AnalysisLegalReferencesUiEvent
import com.example.lagallens.presentation.feature.analysis.legalreferences.viewmodel.AnalysisLegalReferencesViewModel
import com.example.lagallens.presentation.feature.analysis.legalreferences.model.AnalysisLegalReference
import org.junit.Assert.assertEquals
import org.junit.Test

class LegalReferenceSearchTest {
    private fun fixture(): AnalysisLegalReferencesViewModel {
        fun reference(id: String, terms: String) = AnalysisLegalReference(id, 0, 0, 0, 0, 0, 0, terms)
        return AnalysisLegalReferencesViewModel(listOf(
            reference("decree-21-2021-article-15", "nghị định 21 2021 điều 15"),
            reference("commercial-law-2005-article-301", "luật thương mại điều 301"),
            reference("third-fixture", "third reference")
        ))
    }
    @Test fun displayedDecreeNumberMatchesEquivalentSeparators() {
        val viewModel = fixture()
        listOf("21/2021", "21-2021", "21  2021").forEach { query ->
            viewModel.onEvent(AnalysisLegalReferencesUiEvent.QueryChanged(query))
            assertEquals(listOf("decree-21-2021-article-15"), viewModel.uiState.value.references.map { it.id })
        }
    }

    @Test fun unknownQueryDoesNotMatchAndBlankRestoresAll() {
        val viewModel = fixture()
        viewModel.onEvent(AnalysisLegalReferencesUiEvent.QueryChanged("not-a-reference"))
        assertEquals(0, viewModel.uiState.value.references.size)
        viewModel.onEvent(AnalysisLegalReferencesUiEvent.QueryChanged(""))
        assertEquals(3, viewModel.uiState.value.references.size)
    }

    @Test fun vietnameseAccentsAndArticleNumberStillMatch() {
        val viewModel = fixture()
        listOf("LUẬT THƯƠNG MẠI", "điều 301").forEach { query ->
            viewModel.onEvent(AnalysisLegalReferencesUiEvent.QueryChanged(query))
            assertEquals(listOf("commercial-law-2005-article-301"), viewModel.uiState.value.references.map { it.id })
        }
    }
}
