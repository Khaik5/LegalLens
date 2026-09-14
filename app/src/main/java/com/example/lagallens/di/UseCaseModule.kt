package com.example.lagallens.di

import com.example.lagallens.domain.repository.AuthRepository
<<<<<<< Updated upstream
import com.example.lagallens.domain.usecase.AuthenticateUserUseCase
=======
import com.example.lagallens.domain.repository.OnboardingRepository
import com.example.lagallens.domain.repository.ProfileSettingsRepository
import com.example.lagallens.domain.usecase.AuthenticateUserUseCase
import com.example.lagallens.domain.usecase.CompleteOnboardingUseCase
import com.example.lagallens.domain.usecase.IsOnboardingCompletedUseCase
import com.example.lagallens.domain.usecase.ObserveProfileSettingsUseCase
import com.example.lagallens.domain.usecase.UpdateProfileAppearanceUseCase
import com.example.lagallens.domain.usecase.UpdateProfileLanguageUseCase
>>>>>>> Stashed changes

object UseCaseModule {
    fun provideAuthenticateUserUseCase(repository: AuthRepository): AuthenticateUserUseCase {
        return AuthenticateUserUseCase(repository)
    }
<<<<<<< Updated upstream
=======

    fun provideCompleteOnboardingUseCase(repository: OnboardingRepository): CompleteOnboardingUseCase {
        return CompleteOnboardingUseCase(repository)
    }

    fun provideIsOnboardingCompletedUseCase(repository: OnboardingRepository): IsOnboardingCompletedUseCase {
        return IsOnboardingCompletedUseCase(repository)
    }

    fun provideObserveProfileSettingsUseCase(
        repository: ProfileSettingsRepository
    ): ObserveProfileSettingsUseCase = ObserveProfileSettingsUseCase(repository)

    fun provideUpdateProfileAppearanceUseCase(
        repository: ProfileSettingsRepository
    ): UpdateProfileAppearanceUseCase = UpdateProfileAppearanceUseCase(repository)

    fun provideUpdateProfileLanguageUseCase(
        repository: ProfileSettingsRepository
    ): UpdateProfileLanguageUseCase = UpdateProfileLanguageUseCase(repository)
>>>>>>> Stashed changes
}
