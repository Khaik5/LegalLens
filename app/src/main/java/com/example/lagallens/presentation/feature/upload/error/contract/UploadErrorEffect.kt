package com.example.lagallens.presentation.feature.upload.error.contract

sealed interface UploadErrorEffect {
    data object OpenUploadSource : UploadErrorEffect

    data object ReturnToCaller : UploadErrorEffect
}
