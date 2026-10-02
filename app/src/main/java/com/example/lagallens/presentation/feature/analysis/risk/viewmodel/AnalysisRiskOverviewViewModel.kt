package com.example.lagallens.presentation.feature.analysis.risk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.risk.contract.AnalysisRiskOverviewUiEffect
import com.example.lagallens.presentation.feature.analysis.risk.contract.AnalysisRiskOverviewUiEvent
import com.example.lagallens.presentation.feature.analysis.risk.contract.AnalysisRiskOverviewUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisRiskOverviewViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisRiskOverviewUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisRiskOverviewUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: AnalysisRiskOverviewUiEvent) {
        when (event) {
            is AnalysisRiskOverviewUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisRiskOverviewUiEvent.BackClicked -> emitEffect(AnalysisRiskOverviewUiEffect.NavigateBack)

            AnalysisRiskOverviewUiEvent.ViewDetailsClicked -> emitEffect(
                AnalysisRiskOverviewUiEffect.NavigateToFindings(
                    documentName = uiState.value.documentName,
                    documentMetadata = uiState.value.documentMetadata
                )
            )
        }
    }

    private fun emitEffect(effect: AnalysisRiskOverviewUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
