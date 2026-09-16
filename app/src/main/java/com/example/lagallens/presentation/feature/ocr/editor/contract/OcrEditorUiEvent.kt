package com.example.lagallens.presentation.feature.ocr.editor.contract

sealed interface OcrEditorUiEvent {
    data class Initialize(val originalText: String) : OcrEditorUiEvent
    data object BackClicked : OcrEditorUiEvent
    data class TextChanged(val text: String) : OcrEditorUiEvent
    data object RestoreOriginalClicked : OcrEditorUiEvent
    data object SaveClicked : OcrEditorUiEvent
}
