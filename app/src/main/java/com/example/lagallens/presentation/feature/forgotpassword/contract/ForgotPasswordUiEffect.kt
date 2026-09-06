package com.example.lagallens.presentation.feature.forgotpassword.contract

sealed interface ForgotPasswordUiEffect {
    data object CloseScreen : ForgotPasswordUiEffect
    data object NavigateToLogin : ForgotPasswordUiEffect
    data class NavigateToOtp(val email: String) : ForgotPasswordUiEffect
}
