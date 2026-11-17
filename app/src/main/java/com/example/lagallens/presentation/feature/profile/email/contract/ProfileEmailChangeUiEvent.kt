package com.example.lagallens.presentation.feature.profile.email.contract

sealed interface ProfileEmailChangeUiEvent {
    data class NewEmailChanged(val value: String) : ProfileEmailChangeUiEvent
    data class CurrentPasswordChanged(val value: String) : ProfileEmailChangeUiEvent
    data class CodeChanged(val value: String) : ProfileEmailChangeUiEvent
    data object SendCodeClicked : ProfileEmailChangeUiEvent
    data object ConfirmClicked : ProfileEmailChangeUiEvent
}
