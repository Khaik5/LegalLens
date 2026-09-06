package com.example.lagallens.presentation.feature.splash.contract

sealed interface SplashEffect {
    data object OpenOnboarding : SplashEffect
}
