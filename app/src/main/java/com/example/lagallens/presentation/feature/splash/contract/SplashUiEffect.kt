package com.example.lagallens.presentation.feature.splash.contract

sealed interface SplashUiEffect {
    data object NavigateToOnboarding : SplashUiEffect
    data object NavigateToHome : SplashUiEffect
}
