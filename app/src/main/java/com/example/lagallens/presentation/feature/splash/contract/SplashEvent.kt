package com.example.lagallens.presentation.feature.splash.contract

sealed interface SplashEvent {
    data object ScreenStarted : SplashEvent
}
