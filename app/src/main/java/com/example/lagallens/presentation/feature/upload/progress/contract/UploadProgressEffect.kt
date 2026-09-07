package com.example.lagallens.presentation.feature.upload.progress.contract

sealed interface UploadProgressEffect {
    data object NavigateBack : UploadProgressEffect
}
