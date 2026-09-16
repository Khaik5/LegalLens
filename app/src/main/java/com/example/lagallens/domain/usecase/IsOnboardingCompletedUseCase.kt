package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.repository.OnboardingRepository

class IsOnboardingCompletedUseCase(
    private val onboardingRepository: OnboardingRepository
) {
    suspend operator fun invoke(): Boolean = onboardingRepository.isCompleted()
}
