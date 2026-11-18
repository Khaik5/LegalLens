package com.example.lagallens.presentation.feature.ocr.editor.contract

sealed interface OcrEditorUiEvent {
    /** [editedText] differs from [originalText] when the editing is resumed after the process was killed. */
    data class Initialize(val originalText: String, val editedText: String = originalText) : OcrEditorUiEvent
    data object BackClicked : OcrEditorUiEvent
    data object DiscardConfirmed : OcrEditorUiEvent
    data class TextChanged(val text: String) : OcrEditorUiEvent
    data object RestoreOriginalClicked : OcrEditorUiEvent
    data object SaveClicked : OcrEditorUiEvent
}
