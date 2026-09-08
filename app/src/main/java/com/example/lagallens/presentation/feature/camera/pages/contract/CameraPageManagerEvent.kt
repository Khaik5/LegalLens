package com.example.lagallens.presentation.feature.camera.pages.contract

sealed interface CameraPageManagerEvent {
    data object BackClicked : CameraPageManagerEvent
    data object AddPageClicked : CameraPageManagerEvent
    data object RetakeLastClicked : CameraPageManagerEvent
    data object ClearAllClicked : CameraPageManagerEvent
    data class DeletePageClicked(val path: String) : CameraPageManagerEvent
    data object CompleteClicked : CameraPageManagerEvent
}
