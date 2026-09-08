package com.example.lagallens.di

import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.repository.OnboardingRepository
import com.example.lagallens.domain.usecase.AuthenticateUserUseCase
import com.example.lagallens.domain.usecase.CompleteOnboardingUseCase
import com.example.lagallens.domain.usecase.IsOnboardingCompletedUseCase

object UseCaseModule {
    fun provideAuthenticateUserUseCase(repository: AuthRepository): AuthenticateUserUseCase {
        return AuthenticateUserUseCase(repository)
    }

    fun provideCompleteOnboardingUseCase(repository: OnboardingRepository): CompleteOnboardingUseCase {
        return CompleteOnboardingUseCase(repository)
    }

    fun provideIsOnboardingCompletedUseCase(repository: OnboardingRepository): IsOnboardingCompletedUseCase {
        return IsOnboardingCompletedUseCase(repository)
    }
}
