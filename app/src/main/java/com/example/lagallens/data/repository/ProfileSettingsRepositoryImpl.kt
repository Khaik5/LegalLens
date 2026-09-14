package com.example.lagallens.data.repository

import com.example.lagallens.data.datasource.local.datastore.ProfileSettingsLocalDataSource
import com.example.lagallens.domain.model.ProfileAppearance
import com.example.lagallens.domain.model.ProfileLanguage
import com.example.lagallens.domain.model.ProfileSettings
import com.example.lagallens.domain.repository.ProfileSettingsRepository
import kotlinx.coroutines.flow.Flow

class ProfileSettingsRepositoryImpl(
    private val localDataSource: ProfileSettingsLocalDataSource
) : ProfileSettingsRepository {
    override fun observeSettings(): Flow<ProfileSettings> = localDataSource.observeSettings()

    override suspend fun updateAppearance(appearance: ProfileAppearance) {
        localDataSource.updateAppearance(appearance)
    }

    override suspend fun updateLanguage(language: ProfileLanguage) {
        localDataSource.updateLanguage(language)
    }
}
