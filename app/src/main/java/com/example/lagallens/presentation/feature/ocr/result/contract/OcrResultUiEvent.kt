package com.example.lagallens.presentation.feature.ocr.result.contract

sealed interface OcrResultUiEvent {
    data class ScreenOpened(
        val documentName: String,
        val documentDetails: String,
        val documentId: String = "",
        val contractId: String? = null,
        val initialContent: String = "",
        val documentUri: String? = null
    ) : OcrResultUiEvent

    data object BackClicked : OcrResultUiEvent
    data object EditClicked : OcrResultUiEvent
    data object ReviewClicked : OcrResultUiEvent
    data class DocumentUpdated(val documentText: String) : OcrResultUiEvent
    data class ReviewConfirmed(val documentText: String? = null, val documentId: String? = null, val contractId: String? = null) : OcrResultUiEvent
}
