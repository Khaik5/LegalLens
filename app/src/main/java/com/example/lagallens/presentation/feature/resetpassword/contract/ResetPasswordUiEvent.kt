package com.example.lagallens.presentation.feature.resetpassword.contract

sealed interface ResetPasswordUiEvent {
    data class Initialized(val challengeId: String, val verificationToken: String) : ResetPasswordUiEvent
    data class PasswordChanged(val value: String) : ResetPasswordUiEvent
    data class ConfirmPasswordChanged(val value: String) : ResetPasswordUiEvent
    data object BackClicked : ResetPasswordUiEvent
    data object SubmitClicked : ResetPasswordUiEvent
}
