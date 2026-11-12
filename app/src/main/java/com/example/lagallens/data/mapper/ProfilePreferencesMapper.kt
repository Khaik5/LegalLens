package com.example.lagallens.data.mapper

import com.example.lagallens.data.model.request.PatchProfilePreferencesRequest
import com.example.lagallens.data.model.response.ProfilePreferencesResponse
import com.example.lagallens.domain.model.ProfileAppearance
import com.example.lagallens.domain.model.ProfileLanguage
import com.example.lagallens.domain.model.ProfileSettings

/** Unknown or missing values fall back to the app defaults. */
internal fun ProfilePreferencesResponse.toProfileSettings(): ProfileSettings {
    val defaults = ProfileSettings()
    return ProfileSettings(
        appearance = ProfileAppearance.entries.find { it.name == appearance } ?: defaults.appearance,
        language = ProfileLanguage.entries.find { it.name == language } ?: defaults.language,
        isPushEnabled = isPushEnabled ?: defaults.isPushEnabled,
        isEmailEnabled = isEmailEnabled ?: defaults.isEmailEnabled,
        isInAppEnabled = isInAppEnabled ?: defaults.isInAppEnabled
    )
}

internal fun ProfileSettings.toPatchRequest() = PatchProfilePreferencesRequest(
    appearance = appearance.name,
    language = language.name,
    isPushEnabled = isPushEnabled,
    isEmailEnabled = isEmailEnabled,
    isInAppEnabled = isInAppEnabled
)
