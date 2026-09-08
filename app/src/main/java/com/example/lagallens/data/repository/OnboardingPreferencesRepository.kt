package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.OnboardingPreferencesDataSource
import com.example.lagallens.domain.repository.OnboardingRepository

class OnboardingPreferencesRepository(
    private val onboardingPreferencesDataSource: OnboardingPreferencesDataSource
) : OnboardingRepository {
    override suspend fun isCompleted(): Boolean = onboardingPreferencesDataSource.isCompleted()

    override suspend fun markCompleted() {
        onboardingPreferencesDataSource.markCompleted()
    }
}
