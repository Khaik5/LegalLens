package com.example.lagallens.presentation.feature.profile.settings.contract

sealed interface ProfileSettingsUiEvent {
    data class PushToggled(val isEnabled: Boolean) : ProfileSettingsUiEvent
    data class EmailToggled(val isEnabled: Boolean) : ProfileSettingsUiEvent
    data class InAppToggled(val isEnabled: Boolean) : ProfileSettingsUiEvent
    data class BiometricToggled(val isEnabled: Boolean) : ProfileSettingsUiEvent
    data class AppearanceSelected(val appearance: ProfileAppearance) : ProfileSettingsUiEvent
    data object LanguageClicked : ProfileSettingsUiEvent
    data class LanguageSelected(val language: ProfileLanguage) : ProfileSettingsUiEvent
    data object BackClicked : ProfileSettingsUiEvent
}


