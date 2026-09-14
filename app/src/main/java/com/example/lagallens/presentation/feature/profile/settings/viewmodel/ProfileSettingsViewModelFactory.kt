package com.example.lagallens.presentation.feature.profile.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lagallens.domain.usecase.ObserveProfileSettingsUseCase
import com.example.lagallens.domain.usecase.UpdateProfileAppearanceUseCase
import com.example.lagallens.domain.usecase.UpdateProfileLanguageUseCase

class ProfileSettingsViewModelFactory(
    private val observeProfileSettingsUseCase: ObserveProfileSettingsUseCase,
    private val updateProfileAppearanceUseCase: UpdateProfileAppearanceUseCase,
    private val updateProfileLanguageUseCase: UpdateProfileLanguageUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileSettingsViewModel::class.java)) {
            return ProfileSettingsViewModel(
                observeProfileSettingsUseCase,
                updateProfileAppearanceUseCase,
                updateProfileLanguageUseCase
            ) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
    }
}


