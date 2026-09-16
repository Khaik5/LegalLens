package com.example.lagallens.presentation.feature.login.contract

sealed interface LoginUiEffect {
    data object CloseScreen : LoginUiEffect
    data object NavigateToForgotPassword : LoginUiEffect
    data object NavigateToRegister : LoginUiEffect
    data object NavigateToMain : LoginUiEffect
}
