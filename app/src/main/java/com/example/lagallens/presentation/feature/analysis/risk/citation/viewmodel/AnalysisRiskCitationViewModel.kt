package com.example.lagallens.presentation.feature.analysis.risk.citation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.risk.citation.contract.AnalysisRiskCitationUiEffect
import com.example.lagallens.presentation.feature.analysis.risk.citation.contract.AnalysisRiskCitationUiEvent
import com.example.lagallens.presentation.feature.analysis.risk.citation.contract.AnalysisRiskCitationUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnalysisRiskCitationViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisRiskCitationUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<AnalysisRiskCitationUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: AnalysisRiskCitationUiEvent) {
        when (event) {
            AnalysisRiskCitationUiEvent.BackClicked,
            AnalysisRiskCitationUiEvent.ReturnToRiskClicked -> emitEffect(
                AnalysisRiskCitationUiEffect.NavigateBack
            )
        }
    }

    private fun emitEffect(effect: AnalysisRiskCitationUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
