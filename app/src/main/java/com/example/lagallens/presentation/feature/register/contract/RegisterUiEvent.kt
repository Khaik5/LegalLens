package com.example.lagallens.presentation.feature.register.contract

sealed interface RegisterUiEvent {
    data object BackClicked : RegisterUiEvent
    data class FullNameChanged(val value: String) : RegisterUiEvent
    data class EmailChanged(val value: String) : RegisterUiEvent
    data class PasswordChanged(val value: String) : RegisterUiEvent
    data class ConfirmPasswordChanged(val value: String) : RegisterUiEvent
    data object SubmitClicked : RegisterUiEvent
    data object LoginClicked : RegisterUiEvent
}
