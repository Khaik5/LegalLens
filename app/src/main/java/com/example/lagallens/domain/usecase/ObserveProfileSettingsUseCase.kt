package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.ProfileSettings
import com.example.lagallens.domain.repository.ProfileSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveProfileSettingsUseCase(
    private val profileSettingsRepository: ProfileSettingsRepository
) {
    operator fun invoke(): Flow<ProfileSettings> = profileSettingsRepository.observeSettings()
}
