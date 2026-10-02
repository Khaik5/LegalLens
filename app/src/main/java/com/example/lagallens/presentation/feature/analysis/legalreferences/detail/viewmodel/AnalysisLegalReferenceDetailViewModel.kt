package com.example.lagallens.presentation.feature.analysis.legalreferences.detail.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.legalreferences.detail.contract.AnalysisLegalReferenceDetailUiEffect
import com.example.lagallens.presentation.feature.analysis.legalreferences.detail.contract.AnalysisLegalReferenceDetailUiEvent
import com.example.lagallens.presentation.feature.analysis.legalreferences.detail.contract.AnalysisLegalReferenceDetailUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisLegalReferenceDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisLegalReferenceDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisLegalReferenceDetailUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: AnalysisLegalReferenceDetailUiEvent) {
        when (event) {
            is AnalysisLegalReferenceDetailUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    referenceId = event.referenceId,
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisLegalReferenceDetailUiEvent.BackClicked -> emitEffect(
                AnalysisLegalReferenceDetailUiEffect.NavigateBack
            )

            AnalysisLegalReferenceDetailUiEvent.RelatedRiskClicked -> emitEffect(
                AnalysisLegalReferenceDetailUiEffect.NavigateToRelatedRisk
            )
        }
    }

    private fun emitEffect(effect: AnalysisLegalReferenceDetailUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
