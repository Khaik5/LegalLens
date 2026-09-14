package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.ProfileAppearance
import com.example.lagallens.domain.repository.ProfileSettingsRepository

class UpdateProfileAppearanceUseCase(
    private val profileSettingsRepository: ProfileSettingsRepository
) {
    suspend operator fun invoke(appearance: ProfileAppearance) {
        profileSettingsRepository.updateAppearance(appearance)
    }
}
