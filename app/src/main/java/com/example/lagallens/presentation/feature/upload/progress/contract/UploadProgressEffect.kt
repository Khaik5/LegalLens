package com.example.lagallens.presentation.feature.upload.progress.contract

import com.example.lagallens.domain.model.processing.UploadFailure

sealed interface UploadProgressEffect {
    data object NavigateBack : UploadProgressEffect
    data class NavigateToUploadError(val reason: UploadFailure) : UploadProgressEffect
    data class NavigateToContract(val contractId: String) : UploadProgressEffect

    data class NavigateToOcrProcessing(
        val fileName: String,
        val fileDetails: String,
        val documentUri: String? = null,
        val documentId: String = "",
        val mimeType: String = "",
        /** Set when the document is already stored in a backend contract. */
        val contractId: String? = null
    ) : UploadProgressEffect
}
