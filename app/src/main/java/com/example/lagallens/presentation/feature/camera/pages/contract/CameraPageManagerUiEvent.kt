package com.example.lagallens.presentation.feature.camera.pages.contract

sealed interface CameraPageManagerUiEvent {
    data object BackClicked : CameraPageManagerUiEvent
    data object AddPageClicked : CameraPageManagerUiEvent
    data object RetakeLastClicked : CameraPageManagerUiEvent
    data object ClearAllClicked : CameraPageManagerUiEvent
    data class DeletePageClicked(val path: String) : CameraPageManagerUiEvent
    data object CompleteClicked : CameraPageManagerUiEvent
}
