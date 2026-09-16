package com.example.lagallens.presentation.feature.onboarding.contract

sealed interface OnboardingUiEvent {
    data object NextClicked : OnboardingUiEvent
    data object SkipClicked : OnboardingUiEvent
    data class PageSelected(val pageIndex: Int) : OnboardingUiEvent
}
