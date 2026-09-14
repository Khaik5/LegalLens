package com.example.lagallens.presentation.feature.profile.password.contract

sealed interface ProfilePasswordUiEvent {
    data class CurrentPasswordChanged(val value: String) : ProfilePasswordUiEvent

    data class NewPasswordChanged(val value: String) : ProfilePasswordUiEvent

    data class ConfirmationChanged(val value: String) : ProfilePasswordUiEvent

    data object SubmitClicked : ProfilePasswordUiEvent

    data object BackClicked : ProfilePasswordUiEvent
}


