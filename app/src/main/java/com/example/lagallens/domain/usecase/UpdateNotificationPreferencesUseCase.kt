package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.repository.ProfileSettingsRepository

class UpdateNotificationPreferencesUseCase(
    private val profileSettingsRepository: ProfileSettingsRepository
) {
    suspend operator fun invoke(isPushEnabled: Boolean, isEmailEnabled: Boolean, isInAppEnabled: Boolean) {
        profileSettingsRepository.updateNotificationPreferences(isPushEnabled, isEmailEnabled, isInAppEnabled)
    }
}
