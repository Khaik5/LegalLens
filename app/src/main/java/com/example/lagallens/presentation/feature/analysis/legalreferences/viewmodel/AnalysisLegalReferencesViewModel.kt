package com.example.lagallens.presentation.feature.analysis.legalreferences.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.analysis.legalreferences.contract.AnalysisLegalReferencesUiEffect
import com.example.lagallens.presentation.feature.analysis.legalreferences.contract.AnalysisLegalReferencesUiEvent
import com.example.lagallens.presentation.feature.analysis.legalreferences.contract.AnalysisLegalReferencesUiState
import com.example.lagallens.presentation.feature.analysis.legalreferences.model.AnalysisLegalReference
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisLegalReferencesViewModel : ViewModel() {
    private val allReferences = listOf(
        AnalysisLegalReference(
            id = "commercial-law-2005-article-301",
            titleRes = R.string.analysis_legal_references_commercial_law_title,
            countryRes = R.string.analysis_legal_references_country_vietnam,
            articleRes = R.string.analysis_legal_references_commercial_law_article,
            topicRes = R.string.analysis_legal_references_commercial_law_topic,
            descriptionRes = R.string.analysis_legal_references_commercial_law_description,
            effectiveDateRes = R.string.analysis_legal_references_commercial_law_effective_date,
            searchTerms = "luật thương mại 2005 điều 301 mức phạt vi phạm việt nam nghĩa vụ hợp đồng các bên thoả thuận không quá 8%"
        ),
        AnalysisLegalReference(
            id = "civil-code-2015-article-418",
            titleRes = R.string.analysis_legal_references_civil_code_title,
            countryRes = R.string.analysis_legal_references_country_vietnam,
            articleRes = R.string.analysis_legal_references_civil_code_article,
            topicRes = R.string.analysis_legal_references_civil_code_topic,
            descriptionRes = R.string.analysis_legal_references_civil_code_description,
            effectiveDateRes = R.string.analysis_legal_references_civil_code_effective_date,
            searchTerms = "bộ luật dân sự 2015 điều 418 thỏa thuận thoả thuận phạt vi phạm việt nam bồi thường thiệt hại"
        ),
        AnalysisLegalReference(
            id = "decree-21-2021-article-15",
            titleRes = R.string.analysis_legal_references_decree_title,
            countryRes = R.string.analysis_legal_references_country_vietnam,
            articleRes = R.string.analysis_legal_references_decree_article,
            topicRes = R.string.analysis_legal_references_decree_topic,
            descriptionRes = R.string.analysis_legal_references_decree_description,
            effectiveDateRes = R.string.analysis_legal_references_decree_effective_date,
            searchTerms = "nghị định 21 2021 nđ-cp điều 15 bảo đảm thực hiện nghĩa vụ việt nam thỏa thuận thoả thuận bồi thường thiệt hại"
        )
    )

    private val _uiState = MutableStateFlow(
        AnalysisLegalReferencesUiState(references = allReferences)
    )
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisLegalReferencesUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: AnalysisLegalReferencesUiEvent) {
        when (event) {
            is AnalysisLegalReferencesUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            is AnalysisLegalReferencesUiEvent.QueryChanged -> updateQuery(event.query)
            is AnalysisLegalReferencesUiEvent.ReferenceClicked -> {
                val state = _uiState.value
                if (allReferences.any { it.id == event.referenceId }) {
                    emitEffect(AnalysisLegalReferencesUiEffect.NavigateToDetail(
                        referenceId = event.referenceId,
                        documentName = state.documentName,
                        documentMetadata = state.documentMetadata
                    ))
                }
            }

            AnalysisLegalReferencesUiEvent.BackClicked -> emitEffect(
                AnalysisLegalReferencesUiEffect.NavigateBack
            )
        }
    }

    private fun updateQuery(query: String) {
        val normalizedQuery = normalizeSearch(query)
        _uiState.update {
            it.copy(
                query = query,
                references = if (normalizedQuery.isBlank()) {
                    allReferences
                } else {
                    allReferences.filter { reference ->
                        normalizeSearch(reference.searchTerms).contains(normalizedQuery)
                    }
                }
            )
        }
    }

    private fun normalizeSearch(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), " ")
        .trim()

    private fun emitEffect(effect: AnalysisLegalReferencesUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

}
