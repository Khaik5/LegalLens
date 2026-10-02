package com.example.lagallens.presentation.feature.upload.progress.contract

data class UploadProgressUiState(
    val progress: Int = DEFAULT_PROGRESS,
    val fileName: String = "",
    val fileDetails: String = ""
) {
    companion object {
        const val DEFAULT_PROGRESS = 68
    }
}
