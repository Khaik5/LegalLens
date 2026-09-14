package com.example.lagallens.di

import com.example.lagallens.data.datasource.local.datastore.AuthLocalDataSource
<<<<<<< Updated upstream
import com.example.lagallens.data.repository.LocalAuthRepository
import com.example.lagallens.domain.repository.AuthRepository
=======
import com.example.lagallens.data.datasource.local.datastore.OnboardingPreferencesDataSource
import com.example.lagallens.data.datasource.local.datastore.ProfileSettingsLocalDataSource
import com.example.lagallens.data.repository.LocalAuthRepository
import com.example.lagallens.data.repository.OnboardingPreferencesRepository
import com.example.lagallens.data.repository.ProfileSettingsRepositoryImpl
import com.example.lagallens.domain.repository.AuthRepository
import com.example.lagallens.domain.repository.OnboardingRepository
import com.example.lagallens.domain.repository.ProfileSettingsRepository
>>>>>>> Stashed changes

object RepositoryModule {
    fun provideAuthRepository(): AuthRepository {
        return LocalAuthRepository(AuthLocalDataSource())
    }
<<<<<<< Updated upstream
=======

    fun provideOnboardingRepository(context: Context): OnboardingRepository {
        return OnboardingPreferencesRepository(OnboardingPreferencesDataSource(context))
    }

    fun provideProfileSettingsRepository(context: Context): ProfileSettingsRepository {
        return ProfileSettingsRepositoryImpl(ProfileSettingsLocalDataSource(context))
    }
>>>>>>> Stashed changes
}
