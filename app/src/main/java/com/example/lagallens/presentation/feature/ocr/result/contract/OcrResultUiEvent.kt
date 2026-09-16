package com.example.lagallens.presentation.feature.ocr.result.contract

sealed interface OcrResultUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentDetails: String
    ) : OcrResultUiEvent

    data object BackClicked : OcrResultUiEvent
    data object EditClicked : OcrResultUiEvent
    data object ReviewClicked : OcrResultUiEvent
    data class DocumentUpdated(val documentText: String) : OcrResultUiEvent
    data object ReviewConfirmed : OcrResultUiEvent
}
