package com.example.lagallens.presentation.feature.camera.permission.contract

sealed interface CameraPermissionUiEvent {
    data object AllowClicked : CameraPermissionUiEvent
    data object LaterClicked : CameraPermissionUiEvent
    data class PermissionResult(val granted: Boolean) : CameraPermissionUiEvent
}
