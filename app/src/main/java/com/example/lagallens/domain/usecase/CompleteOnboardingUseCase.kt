package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.repository.OnboardingRepository

class CompleteOnboardingUseCase(
    private val onboardingRepository: OnboardingRepository
) {
    suspend operator fun invoke() {
        onboardingRepository.markCompleted()
    }
}
