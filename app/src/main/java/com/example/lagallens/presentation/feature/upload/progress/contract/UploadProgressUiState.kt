package com.example.lagallens.presentation.feature.upload.progress.contract

data class UploadProgressUiState(
    val progress: Int = 0,
    val fileName: String = "",
    val fileDetails: String = "",
    val documentUri: String? = null,
    val documentId: String = ""
)
