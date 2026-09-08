package com.example.lagallens.presentation.feature.camera.permission.contract

sealed interface CameraPermissionEffect {
    data object RequestCameraPermission : CameraPermissionEffect
    data object NavigateBack : CameraPermissionEffect
    data object NavigateToCameraCapture : CameraPermissionEffect
    data object ShowPermissionDenied : CameraPermissionEffect
}
