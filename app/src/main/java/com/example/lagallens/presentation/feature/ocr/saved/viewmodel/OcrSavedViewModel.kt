package com.example.lagallens.presentation.feature.ocr.saved.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.ocr.saved.contract.OcrSavedUiEffect
import com.example.lagallens.presentation.feature.ocr.saved.contract.OcrSavedUiEvent
import com.example.lagallens.presentation.feature.ocr.saved.contract.OcrSavedUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OcrSavedViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OcrSavedUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<OcrSavedUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: OcrSavedUiEvent) {
        when (event) {
            is OcrSavedUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentVersion = event.documentVersion,
                    documentStatus = event.documentStatus,
                    documentId = event.documentId,
                    contractId = event.contractId
                )
            }

            OcrSavedUiEvent.ReturnToContractClicked -> emitEffect(
                _uiState.value.contractId.takeIf(String::isNotBlank)
                    ?.let(OcrSavedUiEffect::NavigateToContract)
                    ?: OcrSavedUiEffect.NavigateBack
            )
            OcrSavedUiEvent.AnalyzeClicked -> {
                emitEffect(OcrSavedUiEffect.NavigateToAnalysis(_uiState.value.documentName, _uiState.value.documentId, _uiState.value.contractId))
            }
        }
    }

    private fun emitEffect(effect: OcrSavedUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
