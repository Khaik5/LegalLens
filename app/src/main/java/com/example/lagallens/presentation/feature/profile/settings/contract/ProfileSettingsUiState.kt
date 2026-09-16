package com.example.lagallens.presentation.feature.profile.settings.contract

data class ProfileSettingsUiState(
    val isPushEnabled: Boolean = true,
    val isEmailEnabled: Boolean = true,
    val isInAppEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = true,
    val appearance: ProfileAppearance = ProfileAppearance.SYSTEM,
    val language: ProfileLanguage = ProfileLanguage.VIETNAMESE
)

enum class ProfileAppearance {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ProfileLanguage {
    VIETNAMESE,
    ENGLISH,
    JAPANESE,
    FRENCH,
    CHINESE,
    KOREAN
}
