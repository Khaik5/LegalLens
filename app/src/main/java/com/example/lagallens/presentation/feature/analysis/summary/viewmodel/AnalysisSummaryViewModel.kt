package com.example.lagallens.presentation.feature.analysis.summary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.summary.contract.AnalysisSummaryUiEffect
import com.example.lagallens.presentation.feature.analysis.summary.contract.AnalysisSummaryUiEvent
import com.example.lagallens.presentation.feature.analysis.summary.contract.AnalysisSummaryUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisSummaryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisSummaryUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisSummaryUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: AnalysisSummaryUiEvent) {
        when (event) {
            is AnalysisSummaryUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisSummaryUiEvent.BackClicked -> emitEffect(AnalysisSummaryUiEffect.NavigateBack)

            AnalysisSummaryUiEvent.ImportantInformationClicked -> emitEffect(
                AnalysisSummaryUiEffect.NavigateToKeyFacts(
                    documentName = uiState.value.documentName,
                    documentMetadata = uiState.value.documentMetadata
                )
            )
        }
    }

    private fun emitEffect(effect: AnalysisSummaryUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
