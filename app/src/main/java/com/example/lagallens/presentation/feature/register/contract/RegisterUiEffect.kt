package com.example.lagallens.presentation.feature.register.contract

sealed interface RegisterUiEffect {
    data object CloseScreen : RegisterUiEffect
    data object NavigateToLogin : RegisterUiEffect
    data class NavigateToOtp(val email: String) : RegisterUiEffect
}
