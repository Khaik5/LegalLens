package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.ProfileLanguage
import com.example.lagallens.domain.repository.ProfileSettingsRepository

class UpdateProfileLanguageUseCase(
    private val profileSettingsRepository: ProfileSettingsRepository
) {
    suspend operator fun invoke(language: ProfileLanguage) {
        profileSettingsRepository.updateLanguage(language)
    }
}
