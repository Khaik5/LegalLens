package com.example.lagallens.presentation.feature.upload.error.contract

sealed interface UploadErrorEvent {
    data class ScreenOpened(
        val errorCode: String,
        val errorMessage: String
    ) : UploadErrorEvent

    data object RetryClicked : UploadErrorEvent

    data object BackToListClicked : UploadErrorEvent

    data object BackClicked : UploadErrorEvent
}
