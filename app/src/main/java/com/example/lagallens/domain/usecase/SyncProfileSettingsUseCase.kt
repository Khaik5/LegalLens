package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.repository.ProfileSettingsRepository

class SyncProfileSettingsUseCase(private val repository: ProfileSettingsRepository) {
    suspend operator fun invoke() = repository.syncFromServer()
}
