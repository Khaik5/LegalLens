package com.example.lagallens.presentation.feature.camera.permission.contract

sealed interface CameraPermissionEvent {
    data object AllowClicked : CameraPermissionEvent
    data object LaterClicked : CameraPermissionEvent
    data class PermissionResult(val granted: Boolean) : CameraPermissionEvent
}
