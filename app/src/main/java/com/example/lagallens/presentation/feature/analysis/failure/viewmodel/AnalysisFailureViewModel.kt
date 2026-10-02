package com.example.lagallens.presentation.feature.analysis.failure.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.failure.contract.AnalysisFailureUiEffect
import com.example.lagallens.presentation.feature.analysis.failure.contract.AnalysisFailureUiEvent
import com.example.lagallens.presentation.feature.analysis.failure.contract.AnalysisFailureUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisFailureViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisFailureUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisFailureUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: AnalysisFailureUiEvent) {
        when (event) {
            is AnalysisFailureUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisFailureUiEvent.BackClicked -> emitEffect(AnalysisFailureUiEffect.NavigateBack)
            AnalysisFailureUiEvent.ReturnHomeClicked -> emitEffect(AnalysisFailureUiEffect.NavigateToHome)
            AnalysisFailureUiEvent.RetryClicked -> emitEffect(
                AnalysisFailureUiEffect.RetryAnalysis(
                    documentName = _uiState.value.documentName,
                    documentMetadata = _uiState.value.documentMetadata
                )
            )
        }
    }

    private fun emitEffect(effect: AnalysisFailureUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
