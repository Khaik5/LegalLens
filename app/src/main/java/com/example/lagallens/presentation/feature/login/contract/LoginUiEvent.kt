package com.example.lagallens.presentation.feature.login.contract

sealed interface LoginUiEvent {
    data object BackClicked : LoginUiEvent
    data class EmailChanged(val value: String) : LoginUiEvent
    data class PasswordChanged(val value: String) : LoginUiEvent
    data object PasswordVisibilityClicked : LoginUiEvent
    data object ForgotPasswordClicked : LoginUiEvent
    data object RegisterClicked : LoginUiEvent
    data object SubmitClicked : LoginUiEvent
}
