package com.example.lagallens.presentation.feature.upload.progress.contract

sealed interface UploadProgressEffect {
    data object NavigateBack : UploadProgressEffect

    data class NavigateToOcrProcessing(
        val fileName: String,
        val fileDetails: String,
        val documentUri: String? = null,
        val documentId: String = ""
    ) : UploadProgressEffect
}
