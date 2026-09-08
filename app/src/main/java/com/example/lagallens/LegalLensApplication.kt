package com.example.lagallens

import android.app.Application
import com.example.lagallens.di.RepositoryModule
import com.example.lagallens.di.UseCaseModule
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.repository.OnboardingRepository
import com.example.lagallens.domain.usecase.AuthenticateUserUseCase
import com.example.lagallens.domain.usecase.CompleteOnboardingUseCase
import com.example.lagallens.domain.usecase.IsOnboardingCompletedUseCase

class LegalLensApplication : Application() {
    private val authRepository: AuthRepository by lazy { RepositoryModule.provideAuthRepository() }
    val authenticateUserUseCase: AuthenticateUserUseCase by lazy {
        UseCaseModule.provideAuthenticateUserUseCase(authRepository)
    }
    private val onboardingRepository: OnboardingRepository by lazy {
        RepositoryModule.provideOnboardingRepository(applicationContext)
    }
    val completeOnboardingUseCase: CompleteOnboardingUseCase by lazy {
        UseCaseModule.provideCompleteOnboardingUseCase(onboardingRepository)
    }
    val isOnboardingCompletedUseCase: IsOnboardingCompletedUseCase by lazy {
        UseCaseModule.provideIsOnboardingCompletedUseCase(onboardingRepository)
    }
}
