package com.example.lagallens.presentation.feature.analysis.risk.detail.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.risk.detail.contract.AnalysisRiskDetailUiEffect
import com.example.lagallens.presentation.feature.analysis.risk.detail.contract.AnalysisRiskDetailUiEvent
import com.example.lagallens.presentation.feature.analysis.risk.detail.contract.AnalysisRiskDetailUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnalysisRiskDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisRiskDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisRiskDetailUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: AnalysisRiskDetailUiEvent) {
        when (event) {
            AnalysisRiskDetailUiEvent.BackClicked -> emitEffect(AnalysisRiskDetailUiEffect.NavigateBack)
            AnalysisRiskDetailUiEvent.LegalReferenceClicked -> emitEffect(
                AnalysisRiskDetailUiEffect.NavigateToCitation
            )
        }
    }

    private fun emitEffect(effect: AnalysisRiskDetailUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
