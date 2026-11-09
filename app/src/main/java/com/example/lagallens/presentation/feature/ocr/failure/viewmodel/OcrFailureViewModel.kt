package com.example.lagallens.presentation.feature.ocr.failure.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.ocr.failure.contract.OcrFailureUiEffect
import com.example.lagallens.presentation.feature.ocr.failure.contract.OcrFailureUiEvent
import com.example.lagallens.presentation.feature.ocr.failure.contract.OcrFailureUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OcrFailureViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OcrFailureUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<OcrFailureUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: OcrFailureUiEvent) {
        when (event) {
            is OcrFailureUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentDetails = event.documentDetails
                )
            }

            OcrFailureUiEvent.BackClicked -> emitEffect(OcrFailureUiEffect.NavigateBack)
            OcrFailureUiEvent.RetryClicked -> emitEffect(
                OcrFailureUiEffect.RetryOcr(
                    documentName = _uiState.value.documentName,
                    documentDetails = _uiState.value.documentDetails
                )
            )
        }
    }

    private fun emitEffect(effect: OcrFailureUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
