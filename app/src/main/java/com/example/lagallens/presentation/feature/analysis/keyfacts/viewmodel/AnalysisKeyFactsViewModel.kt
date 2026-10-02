package com.example.lagallens.presentation.feature.analysis.keyfacts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisKeyFactsUiEffect
import com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisKeyFactsUiEvent
import com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisKeyFactsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisKeyFactsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisKeyFactsUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisKeyFactsUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: AnalysisKeyFactsUiEvent) {
        when (event) {
            is AnalysisKeyFactsUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisKeyFactsUiEvent.BackClicked -> emitEffect(AnalysisKeyFactsUiEffect.NavigateBack)

            is AnalysisKeyFactsUiEvent.SourceClicked -> emitEffect(
                AnalysisKeyFactsUiEffect.ShowClauseSource(event.source)
            )
        }
    }

    private fun emitEffect(effect: AnalysisKeyFactsUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
