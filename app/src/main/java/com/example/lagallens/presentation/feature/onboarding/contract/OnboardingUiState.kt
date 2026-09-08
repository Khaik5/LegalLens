package com.example.lagallens.presentation.feature.onboarding.contract

import com.example.lagallens.R

data class OnboardingUiState(
    val pages: List<OnboardingPage> = listOf(
        OnboardingPage(
            imageResId = R.drawable.onboarding_illustration_01,
            titleResId = R.string.onboarding_title_search,
            descriptionResId = R.string.onboarding_description_search,
            actionResId = R.string.onboarding_next
        ),
        OnboardingPage(
            imageResId = R.drawable.onboarding_illustration_02,
            titleResId = R.string.onboarding_title_assistant,
            descriptionResId = R.string.onboarding_description_assistant,
            actionResId = R.string.onboarding_next
        ),
        OnboardingPage(
            imageResId = R.drawable.onboarding_illustration_03,
            titleResId = R.string.onboarding_title_manage,
            descriptionResId = R.string.onboarding_description_manage,
            actionResId = R.string.onboarding_start
        )
    ),
    val pageIndex: Int = 0
) {
    val page: OnboardingPage
        get() = pages[pageIndex]
}
