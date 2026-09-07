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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OcrReviewViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OcrReviewUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<OcrReviewEffect>()
    val effects = _effects.asSharedFlow()

    fun onEvent(event: OcrReviewEvent) {
        when (event) {
            OcrReviewEvent.BackClicked -> emitEffect(OcrReviewEffect.NavigateBack)
            is OcrReviewEvent.TextChanged -> _uiState.update {
                it.copy(text = event.text, isOriginalText = event.text == OcrReviewSample.originalText)
            }
            OcrReviewEvent.RestoreOriginalClicked -> _uiState.update {
                it.copy(text = OcrReviewSample.originalText, isOriginalText = true)
            }
            OcrReviewEvent.SaveClicked -> emitEffect(OcrReviewEffect.ShowSaved)
        }
    }

    private fun emitEffect(effect: OcrReviewEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
