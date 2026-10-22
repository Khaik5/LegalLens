package com.example.lagallens.di

import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.repository.ChatbotRepository
import com.example.lagallens.domain.repository.AnalysisRepository
import com.example.lagallens.domain.repository.OcrRepository
import com.example.lagallens.domain.repository.OnboardingRepository
import com.example.lagallens.domain.repository.ProfileSettingsRepository
import com.example.lagallens.domain.usecase.AuthenticateUserUseCase
import com.example.lagallens.domain.usecase.OpenChatbotConversationUseCase
import com.example.lagallens.domain.usecase.RetryChatbotMessageUseCase
import com.example.lagallens.domain.usecase.SendChatbotMessageUseCase
import com.example.lagallens.domain.usecase.GetAnalysisOverviewUseCase
import com.example.lagallens.domain.usecase.GetOcrResultUseCase
import com.example.lagallens.domain.usecase.CompleteOnboardingUseCase
import com.example.lagallens.domain.usecase.IsOnboardingCompletedUseCase
import com.example.lagallens.domain.usecase.ObserveProfileSettingsUseCase
import com.example.lagallens.domain.usecase.UpdateProfileAppearanceUseCase
import com.example.lagallens.domain.usecase.UpdateProfileLanguageUseCase

object UseCaseModule {
    fun provideAuthenticateUserUseCase(repository: AuthRepository): AuthenticateUserUseCase {
        return AuthenticateUserUseCase(repository)
    }

    fun provideOpenChatbotConversationUseCase(
        repository: ChatbotRepository
    ): OpenChatbotConversationUseCase = OpenChatbotConversationUseCase(repository)

    fun provideSendChatbotMessageUseCase(
        repository: ChatbotRepository
    ): SendChatbotMessageUseCase = SendChatbotMessageUseCase(repository)

    fun provideRetryChatbotMessageUseCase(
        repository: ChatbotRepository
    ): RetryChatbotMessageUseCase = RetryChatbotMessageUseCase(repository)

    fun provideGetOcrResultUseCase(repository: OcrRepository): GetOcrResultUseCase =
        GetOcrResultUseCase(repository)

    fun provideGetAnalysisOverviewUseCase(repository: AnalysisRepository): GetAnalysisOverviewUseCase =
        GetAnalysisOverviewUseCase(repository)

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
}
