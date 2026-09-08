package com.example.lagallens.presentation.feature.onboarding.contract

sealed interface OnboardingUiEffect {
    data object NavigateToHome : OnboardingUiEffect
}
