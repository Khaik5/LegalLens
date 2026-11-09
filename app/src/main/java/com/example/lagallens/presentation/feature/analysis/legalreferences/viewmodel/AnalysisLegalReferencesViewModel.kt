package com.example.lagallens.presentation.feature.analysis.legalreferences.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.analysis.legalreferences.contract.AnalysisLegalReferencesUiEffect
import com.example.lagallens.presentation.feature.analysis.legalreferences.contract.AnalysisLegalReferencesUiEvent
import com.example.lagallens.presentation.feature.analysis.legalreferences.contract.AnalysisLegalReferencesUiState
import com.example.lagallens.presentation.feature.analysis.legalreferences.model.AnalysisLegalReference
import java.util.Locale
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// No account-specific employment-law references have been verified in DEMO.
class AnalysisLegalReferencesViewModel(private var allReferences: List<AnalysisLegalReference> = emptyList()) : ViewModel() {
    private var isUnlocked = true

    private val _uiState = MutableStateFlow(
        AnalysisLegalReferencesUiState(references = allReferences)
    )
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<AnalysisLegalReferencesUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun setDemoReferences(references: List<AnalysisLegalReference>, unlocked: Boolean) {
        allReferences = references
        isUnlocked = unlocked
        _uiState.update { it.copy(isDemo = true) }
        updateQuery(_uiState.value.query)
    }

    fun clearResult() {
        allReferences = emptyList()
        _uiState.value = AnalysisLegalReferencesUiState()
    }

    private fun visibleReferences(): List<AnalysisLegalReference> =
        if (isUnlocked || allReferences.isEmpty()) allReferences else allReferences.take(com.example.lagallens.domain.model.analysis.AnalysisPreview.visibleCount(allReferences.size, false))

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
                if (visibleReferences().any { it.id == event.referenceId }) {
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
                    visibleReferences()
                } else {
                    visibleReferences().filter { reference ->
                        normalizeSearch(reference.searchTerms).contains(normalizedQuery)
                    }
                },
                lockedCount = allReferences.size - visibleReferences().size
            )
        }
    }

    private fun normalizeSearch(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), " ")
        .trim()

    private fun emitEffect(effect: AnalysisLegalReferencesUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }

}
