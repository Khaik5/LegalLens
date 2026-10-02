package com.example.lagallens.presentation.feature.ocr.editor.contract

sealed interface OcrEditorUiEffect {
    data object NavigateBack : OcrEditorUiEffect
    data class FinishWithSavedText(val text: String) : OcrEditorUiEffect
}
