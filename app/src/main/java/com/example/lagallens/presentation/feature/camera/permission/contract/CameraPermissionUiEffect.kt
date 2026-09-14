package com.example.lagallens.presentation.feature.camera.permission.contract

sealed interface CameraPermissionUiEffect {
    data object RequestCameraPermission : CameraPermissionUiEffect
    data object NavigateBack : CameraPermissionUiEffect
    data object NavigateToCameraCapture : CameraPermissionUiEffect
    data object ShowPermissionDenied : CameraPermissionUiEffect
}
