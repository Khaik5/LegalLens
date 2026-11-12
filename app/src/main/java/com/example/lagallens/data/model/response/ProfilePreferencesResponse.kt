package com.example.lagallens.data.model.response

data class ProfilePreferencesResponse(
    /** SYSTEM, LIGHT or DARK. */
    val appearance: String?,
    /** VIETNAMESE or ENGLISH. */
    val language: String?,
    val isPushEnabled: Boolean?,
    val isEmailEnabled: Boolean?,
    val isInAppEnabled: Boolean?
)
