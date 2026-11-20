package com.example.lagallens.presentation.feature.onboarding.contract

sealed interface OnboardingUiEffect {
    data object NavigateToHome : OnboardingUiEffect
    /** Completing onboarding could not be stored; the user can tap again. */
    data object ShowSaveFailed : OnboardingUiEffect
}
