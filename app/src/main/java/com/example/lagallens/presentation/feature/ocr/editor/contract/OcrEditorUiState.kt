package com.example.lagallens.presentation.feature.ocr.editor.contract

data class OcrEditorUiState(
    val originalText: String = "",
    val text: String = "",
    val isOriginalText: Boolean = true
)
