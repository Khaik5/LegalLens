package com.example.lagallens.presentation.feature.documentviewer.contract

sealed interface DocumentViewerUiEffect {
    data object NavigateBack : DocumentViewerUiEffect
    data object DownloadFailed : DocumentViewerUiEffect
    data class SaveDownload(val uri: String, val fileName: String, val mimeType: String) : DocumentViewerUiEffect
}
