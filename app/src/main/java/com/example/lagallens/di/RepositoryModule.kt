package com.example.lagallens.di

import android.content.Context
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.data.datasource.local.datastore.OnboardingPreferencesDataSource
import com.example.lagallens.data.datasource.local.datastore.ProfileSettingsLocalDataSource
import com.example.lagallens.data.repository.LocalAuthRepository
import com.example.lagallens.data.repository.FakeChatbotRepository
import com.example.lagallens.data.repository.FakeAnalysisRepository
import com.example.lagallens.data.repository.FakeOcrRepository
import com.example.lagallens.data.repository.OnboardingPreferencesRepository
import com.example.lagallens.data.repository.ProfileSettingsRepositoryImpl
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.repository.ChatbotRepository
import com.example.lagallens.domain.repository.AnalysisRepository
import com.example.lagallens.domain.repository.OcrRepository
import com.example.lagallens.domain.repository.OnboardingRepository
import com.example.lagallens.domain.repository.ProfileSettingsRepository

object RepositoryModule {
    fun provideAuthRepository(): AuthRepository {
        return LocalAuthRepository(AuthLocalDataSource())
    }

    fun provideChatbotRepository(): ChatbotRepository = FakeChatbotRepository()

    fun provideOcrRepository(): OcrRepository = FakeOcrRepository()

    fun provideAnalysisRepository(): AnalysisRepository = FakeAnalysisRepository()

    fun provideOnboardingRepository(context: Context): OnboardingRepository {
        return OnboardingPreferencesRepository(OnboardingPreferencesDataSource(context))
    }

    fun provideProfileSettingsRepository(context: Context): ProfileSettingsRepository {
        return ProfileSettingsRepositoryImpl(ProfileSettingsLocalDataSource(context))
    }
}
