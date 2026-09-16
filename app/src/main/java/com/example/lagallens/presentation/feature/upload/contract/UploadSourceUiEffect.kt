package com.example.lagallens.presentation.feature.upload.contract

sealed interface UploadSourceUiEffect {
    data object NavigateBack : UploadSourceUiEffect
    data object NavigateToCameraPermission : UploadSourceUiEffect
    data class OpenDocumentPicker(val mimeTypes: Array<String>) : UploadSourceUiEffect
    data object OpenImagePicker : UploadSourceUiEffect
    data object ShowFileSelected : UploadSourceUiEffect
}
