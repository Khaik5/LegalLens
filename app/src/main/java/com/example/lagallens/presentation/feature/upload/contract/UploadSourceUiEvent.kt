package com.example.lagallens.presentation.feature.upload.contract

sealed interface UploadSourceUiEvent {
    data object BackClicked : UploadSourceUiEvent
    data object PdfClicked : UploadSourceUiEvent
    data object DocxClicked : UploadSourceUiEvent
    data object GalleryClicked : UploadSourceUiEvent
    data object CameraClicked : UploadSourceUiEvent
    data object FileSelected : UploadSourceUiEvent
}
