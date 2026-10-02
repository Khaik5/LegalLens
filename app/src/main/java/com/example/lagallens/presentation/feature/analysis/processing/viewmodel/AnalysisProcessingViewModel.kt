package com.example.lagallens.presentation.feature.analysis.processing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.processing.contract.AnalysisProcessingUiEffect
import com.example.lagallens.presentation.feature.analysis.processing.contract.AnalysisProcessingUiEvent
import com.example.lagallens.presentation.feature.analysis.processing.contract.AnalysisProcessingUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisProcessingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisProcessingUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisProcessingUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var isCompletionScheduled = false

    fun onEvent(event: AnalysisProcessingUiEvent) {
        when (event) {
            is AnalysisProcessingUiEvent.ScreenOpened -> {
                _uiState.update {
                    it.copy(
                        documentName = event.documentName,
                        documentMetadata = event.documentMetadata
                    )
                }
                scheduleCompletion(event.shouldSimulateFailure)
            }
        }
    }

    private fun scheduleCompletion(shouldSimulateFailure: Boolean) {
        if (isCompletionScheduled) return
        isCompletionScheduled = true
        viewModelScope.launch {
            (3..6).forEach { activeStepId ->
                delay(900)
                _uiState.update { state ->
                    state.withActiveStep(activeStepId)
                }
            }
            delay(900)
            val state = _uiState.value
            val effect = if (shouldSimulateFailure) {
                AnalysisProcessingUiEffect.NavigateToAnalysisFailure(
                    documentName = state.documentName,
                    documentMetadata = state.documentMetadata
                )
            } else {
                AnalysisProcessingUiEffect.NavigateToAnalysisResult(
                    documentName = state.documentName,
                    documentMetadata = state.documentMetadata
                )
            }
            _uiEffect.emit(effect)
        }
    }
}
