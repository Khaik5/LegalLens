package com.example.lagallens.presentation.feature.ocr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewEffect
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewEvent
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewSample
import com.example.lagallens.presentation.feature.ocr.contract.OcrReviewUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OcrReviewViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OcrReviewUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<OcrReviewEffect>()
    val effects = _effects.asSharedFlow()

    fun onEvent(event: OcrReviewEvent) {
        when (event) {
            is OcrReviewEvent.ScreenOpened -> {
                val current = _uiState.value
                if (current.documentId != event.documentId || event.documentId.isBlank()) {
                    _uiState.value = current.copy(documentId = event.documentId, contractId = event.contractId, documentText = event.documentText)
                }
            }
            is OcrReviewEvent.DocumentTextChanged -> _uiState.value = _uiState.value.copy(documentText = event.documentText)
            OcrReviewEvent.BackClicked -> emitEffect(OcrReviewEffect.NavigateBack)
            OcrReviewEvent.ConfirmClicked -> {
                val state = _uiState.value
                emitEffect(OcrReviewEffect.FinishWithConfirmedReview(state.documentId, state.contractId, state.documentText))
            }
        }
    }

    private fun emitEffect(effect: OcrReviewEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
