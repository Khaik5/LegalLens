package com.example.lagallens.presentation.feature.upload.progress.contract

sealed interface UploadProgressEvent {
    data class ScreenOpened(
        val fileName: String,
        val fileDetails: String,
        val documentUri: String? = null,
        val documentId: String = "",
        val mimeType: String = "",
        val scanPageUris: List<String> = emptyList(),
        val sessionKey: String? = null
    ) : UploadProgressEvent

    data object CancelClicked : UploadProgressEvent
}
