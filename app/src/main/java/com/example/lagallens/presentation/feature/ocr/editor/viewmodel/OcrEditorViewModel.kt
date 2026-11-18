package com.example.lagallens.presentation.feature.ocr.editor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.ocr.editor.contract.OcrEditorUiEffect
import com.example.lagallens.presentation.feature.ocr.editor.contract.OcrEditorUiEvent
import com.example.lagallens.presentation.feature.ocr.editor.contract.OcrEditorUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OcrEditorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OcrEditorUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<OcrEditorUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    // The Activity re-sends Initialize on every recreation (rotation, font or theme change) while this
    // ViewModel survives, so only the first call may seed the text; later ones would drop the user's edits.
    private var initialized = false

    fun onEvent(event: OcrEditorUiEvent) {
        when (event) {
            is OcrEditorUiEvent.Initialize -> if (!initialized) {
                initialized = true
                _uiState.update {
                    it.copy(
                        originalText = event.originalText,
                        text = event.editedText,
                        isOriginalText = event.editedText == event.originalText
                    )
                }
            }
            OcrEditorUiEvent.BackClicked ->
                if (_uiState.value.isOriginalText) emitEffect(OcrEditorUiEffect.NavigateBack)
                else emitEffect(OcrEditorUiEffect.ConfirmDiscard)
            OcrEditorUiEvent.DiscardConfirmed -> emitEffect(OcrEditorUiEffect.NavigateBack)
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
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
