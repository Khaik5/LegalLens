package com.example.lagallens.domain.model

data class ProfileSettings(
    val appearance: ProfileAppearance = ProfileAppearance.SYSTEM,
    val language: ProfileLanguage = ProfileLanguage.VIETNAMESE,
    val isPushEnabled: Boolean = true,
    val isEmailEnabled: Boolean = true,
    val isInAppEnabled: Boolean = false
)

enum class ProfileAppearance {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ProfileLanguage {
    VIETNAMESE,
    ENGLISH
}
