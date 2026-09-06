package com.example.lagallens.presentation.feature.upload.contract

sealed interface UploadSourceEvent {
    data object BackClicked : UploadSourceEvent
    data object PdfClicked : UploadSourceEvent
    data object DocxClicked : UploadSourceEvent
    data object GalleryClicked : UploadSourceEvent
    data object CameraClicked : UploadSourceEvent
    data object FileSelected : UploadSourceEvent
}
