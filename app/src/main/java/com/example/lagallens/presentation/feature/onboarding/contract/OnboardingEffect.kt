package com.example.lagallens.presentation.feature.onboarding.contract

sealed interface OnboardingEffect {
    data object StartRequested : OnboardingEffect
    data object SkipRequested : OnboardingEffect
}
