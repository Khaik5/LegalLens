package com.example.lagallens.presentation.feature.documentviewer.contract

sealed interface DocumentViewerUiEvent {
    data class Opened(val title: String, val metadata: String, val pageCount: Int) : DocumentViewerUiEvent
    data object PdfLoading : DocumentViewerUiEvent
    data class PdfLoaded(val title: String, val metadata: String, val pageCount: Int) : DocumentViewerUiEvent
    data object PdfLoadFailed : DocumentViewerUiEvent
    data object RetryClicked : DocumentViewerUiEvent
    data object PreviousClicked : DocumentViewerUiEvent
    data object NextClicked : DocumentViewerUiEvent
    data object ZoomInClicked : DocumentViewerUiEvent
    data object ZoomOutClicked : DocumentViewerUiEvent
    data object BackClicked : DocumentViewerUiEvent
}
