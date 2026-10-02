package com.example.lagallens.presentation.feature.upload.progress.contract

sealed interface UploadProgressEvent {
    data class ScreenOpened(
        val fileName: String,
        val fileDetails: String
    ) : UploadProgressEvent

    data object CancelClicked : UploadProgressEvent
}
