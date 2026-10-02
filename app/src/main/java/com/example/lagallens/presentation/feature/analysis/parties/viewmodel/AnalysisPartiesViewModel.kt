package com.example.lagallens.presentation.feature.analysis.parties.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.parties.contract.AnalysisPartiesUiEffect
import com.example.lagallens.presentation.feature.analysis.parties.contract.AnalysisPartiesUiEvent
import com.example.lagallens.presentation.feature.analysis.parties.contract.AnalysisPartiesUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisPartiesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisPartiesUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisPartiesUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: AnalysisPartiesUiEvent) {
        when (event) {
            is AnalysisPartiesUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisPartiesUiEvent.BackClicked -> emitEffect(AnalysisPartiesUiEffect.NavigateBack)
        }
    }

    private fun emitEffect(effect: AnalysisPartiesUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
