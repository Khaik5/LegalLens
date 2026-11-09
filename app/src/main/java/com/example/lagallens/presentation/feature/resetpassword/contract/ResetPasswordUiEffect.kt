package com.example.lagallens.presentation.feature.resetpassword.contract

sealed interface ResetPasswordUiEffect {
    data object CloseScreen : ResetPasswordUiEffect
    data object NavigateToLogin : ResetPasswordUiEffect
}
