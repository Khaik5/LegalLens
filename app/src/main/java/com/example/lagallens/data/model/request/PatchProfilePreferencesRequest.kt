package com.example.lagallens.data.model.request

/** Only non-null fields are sent, so a request changes exactly the settings it names. */
data class PatchProfilePreferencesRequest(
    val appearance: String? = null,
    val language: String? = null,
    val isPushEnabled: Boolean? = null,
    val isEmailEnabled: Boolean? = null,
    val isInAppEnabled: Boolean? = null
)
