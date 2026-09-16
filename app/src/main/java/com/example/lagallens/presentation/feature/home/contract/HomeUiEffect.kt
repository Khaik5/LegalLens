package com.example.lagallens.presentation.feature.home.contract

sealed interface HomeUiEffect {
    data object NavigateToLogin : HomeUiEffect
    data object NavigateToRegister : HomeUiEffect
}
