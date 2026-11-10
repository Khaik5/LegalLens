package com.example.lagallens.presentation.feature.splash.contract

sealed interface SplashUiEvent {
    data object ScreenStarted : SplashUiEvent
    data object RetryClicked : SplashUiEvent
}
