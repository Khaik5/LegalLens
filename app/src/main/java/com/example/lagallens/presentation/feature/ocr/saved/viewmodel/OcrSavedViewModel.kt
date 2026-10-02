package com.example.lagallens.presentation.feature.ocr.saved.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.ocr.saved.contract.OcrSavedUiEffect
import com.example.lagallens.presentation.feature.ocr.saved.contract.OcrSavedUiEvent
import com.example.lagallens.presentation.feature.ocr.saved.contract.OcrSavedUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OcrSavedViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OcrSavedUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<OcrSavedUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: OcrSavedUiEvent) {
        when (event) {
            is OcrSavedUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentVersion = event.documentVersion,
                    documentStatus = event.documentStatus
                )
            }

            OcrSavedUiEvent.ReturnToContractClicked -> emitEffect(OcrSavedUiEffect.NavigateBack)
            OcrSavedUiEvent.AnalyzeClicked -> {
                emitEffect(OcrSavedUiEffect.NavigateToAnalysis(_uiState.value.documentName))
            }
        }
    }

    private fun emitEffect(effect: OcrSavedUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
