package com.example.lagallens.presentation.feature.analysis.start.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.start.contract.AnalysisStartUiEffect
import com.example.lagallens.presentation.feature.analysis.start.contract.AnalysisStartUiEvent
import com.example.lagallens.presentation.feature.analysis.start.contract.AnalysisStartUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisStartViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisStartUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisStartUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: AnalysisStartUiEvent) {
        when (event) {
            is AnalysisStartUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisStartUiEvent.BackClicked -> emitEffect(AnalysisStartUiEffect.NavigateBack)
            AnalysisStartUiEvent.StartAnalysisClicked -> {
                val state = _uiState.value
                emitEffect(
                    AnalysisStartUiEffect.NavigateToAnalysisProcessing(
                        documentName = state.documentName,
                        documentMetadata = state.documentMetadata
                    )
                )
            }
        }
    }

    private fun emitEffect(effect: AnalysisStartUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
