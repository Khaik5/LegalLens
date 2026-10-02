package com.example.lagallens.presentation.feature.ocr.saved.contract

sealed interface OcrSavedUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentVersion: String,
        val documentStatus: String
    ) : OcrSavedUiEvent

    data object ReturnToContractClicked : OcrSavedUiEvent

    data object AnalyzeClicked : OcrSavedUiEvent
}
