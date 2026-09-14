package com.example.lagallens.presentation.feature.profile.settings.contract

sealed interface ProfileSettingsUiEffect {
    data object NavigateBack : ProfileSettingsUiEffect
    data class ShowLanguagePicker(val language: ProfileLanguage) : ProfileSettingsUiEffect
}


