package com.example.lagallens.presentation.feature.analysis.result.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultDestination
import com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultUiEffect
import com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultUiEvent
import com.example.lagallens.presentation.feature.analysis.result.contract.AnalysisResultUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisResultViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisResultUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisResultUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: AnalysisResultUiEvent) {
        when (event) {
            is AnalysisResultUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisResultUiEvent.BackClicked -> emitEffect(AnalysisResultUiEffect.NavigateBack)

            is AnalysisResultUiEvent.ItemClicked -> when (event.destination) {
                AnalysisResultDestination.SUMMARY -> emitEffect(
                    AnalysisResultUiEffect.NavigateToSummary(
                        documentName = _uiState.value.documentName,
                        documentMetadata = _uiState.value.documentMetadata
                    )
                )

                AnalysisResultDestination.KEY_FACTS -> emitEffect(
                    AnalysisResultUiEffect.NavigateToKeyFacts(
                        documentName = _uiState.value.documentName,
                        documentMetadata = _uiState.value.documentMetadata
                    )
                )

                AnalysisResultDestination.PARTIES -> emitEffect(
                    AnalysisResultUiEffect.NavigateToParties(
                        documentName = _uiState.value.documentName,
                        documentMetadata = _uiState.value.documentMetadata
                    )
                )

                AnalysisResultDestination.OBLIGATIONS -> emitEffect(
                    AnalysisResultUiEffect.NavigateToObligations(
                        documentName = _uiState.value.documentName,
                        documentMetadata = _uiState.value.documentMetadata
                    )
                )

                AnalysisResultDestination.RISK_OVERVIEW -> emitEffect(
                    AnalysisResultUiEffect.NavigateToRiskOverview(
                        documentName = _uiState.value.documentName,
                        documentMetadata = _uiState.value.documentMetadata
                    )
                )

                AnalysisResultDestination.LEGAL_REFERENCES -> emitEffect(
                    AnalysisResultUiEffect.NavigateToLegalReferences(
                        documentName = _uiState.value.documentName,
                        documentMetadata = _uiState.value.documentMetadata
                    )
                )
            }
        }
    }

    private fun emitEffect(effect: AnalysisResultUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
