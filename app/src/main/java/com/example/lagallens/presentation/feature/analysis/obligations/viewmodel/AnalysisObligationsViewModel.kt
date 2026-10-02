package com.example.lagallens.presentation.feature.analysis.obligations.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsUiEffect
import com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsUiEvent
import com.example.lagallens.presentation.feature.analysis.obligations.contract.AnalysisObligationsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisObligationsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisObligationsUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisObligationsUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: AnalysisObligationsUiEvent) {
        when (event) {
            is AnalysisObligationsUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisObligationsUiEvent.BackClicked -> emitEffect(AnalysisObligationsUiEffect.NavigateBack)
        }
    }

    private fun emitEffect(effect: AnalysisObligationsUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
