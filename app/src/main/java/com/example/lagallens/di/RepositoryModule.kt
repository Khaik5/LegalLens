package com.example.lagallens.di

import android.content.Context
import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
import com.example.lagallens.data.datasource.local.datastore.OnboardingPreferencesDataSource
import com.example.lagallens.data.repository.LocalAuthRepository
import com.example.lagallens.data.repository.OnboardingPreferencesRepository
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.repository.OnboardingRepository

object RepositoryModule {
    fun provideAuthRepository(): AuthRepository {
        return LocalAuthRepository(AuthLocalDataSource())
    }

    fun provideOnboardingRepository(context: Context): OnboardingRepository {
        return OnboardingPreferencesRepository(OnboardingPreferencesDataSource(context))
    }
}
