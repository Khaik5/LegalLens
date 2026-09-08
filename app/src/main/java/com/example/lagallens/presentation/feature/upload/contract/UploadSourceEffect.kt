package com.example.lagallens.presentation.feature.upload.contract

sealed interface UploadSourceEffect {
    data object NavigateBack : UploadSourceEffect
    data object NavigateToCameraPermission : UploadSourceEffect
    data class OpenDocumentPicker(val mimeTypes: Array<String>) : UploadSourceEffect
    data object OpenImagePicker : UploadSourceEffect
    data object ShowFileSelected : UploadSourceEffect
}
