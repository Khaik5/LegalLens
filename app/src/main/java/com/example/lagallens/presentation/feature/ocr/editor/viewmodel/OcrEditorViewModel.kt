package com.example.lagallens.presentation.feature.ocr.editor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.ocr.editor.contract.OcrEditorUiEffect
import com.example.lagallens.presentation.feature.ocr.editor.contract.OcrEditorUiEvent
import com.example.lagallens.presentation.feature.ocr.editor.contract.OcrEditorUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OcrEditorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OcrEditorUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<OcrEditorUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: OcrEditorUiEvent) {
        when (event) {
            is OcrEditorUiEvent.Initialize -> _uiState.update {
                it.copy(
                    originalText = event.originalText,
                    text = event.originalText,
                    isOriginalText = true
                )
            }
            OcrEditorUiEvent.BackClicked -> emitEffect(OcrEditorUiEffect.NavigateBack)
            is OcrEditorUiEvent.TextChanged -> _uiState.update {
                it.copy(text = event.text, isOriginalText = event.text == it.originalText)
            }
            OcrEditorUiEvent.RestoreOriginalClicked -> _uiState.update {
                it.copy(text = it.originalText, isOriginalText = true)
            }
            OcrEditorUiEvent.SaveClicked -> emitEffect(OcrEditorUiEffect.FinishWithSavedText(_uiState.value.text))
        }
    }

    private fun emitEffect(effect: OcrEditorUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
