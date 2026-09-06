package com.example.lagallens.presentation.feature.forgotpassword.contract

sealed interface ForgotPasswordUiEvent {
    data object BackClicked : ForgotPasswordUiEvent
    data class EmailChanged(val value: String) : ForgotPasswordUiEvent
    data object SubmitClicked : ForgotPasswordUiEvent
    data object LoginClicked : ForgotPasswordUiEvent
}
