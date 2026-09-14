package com.example.lagallens.domain.model

data class ProfileSettings(
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


