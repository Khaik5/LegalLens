package com.example.lagallens.presentation.feature.onboarding.contract

sealed interface OnboardingEvent {
    data object NextClicked : OnboardingEvent
    data object SkipClicked : OnboardingEvent
    data class PageSelected(val pageIndex: Int) : OnboardingEvent
}
