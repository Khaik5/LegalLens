package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.ProfileAppearance
import com.example.lagallens.domain.model.ProfileLanguage
import com.example.lagallens.domain.model.ProfileSettings
import kotlinx.coroutines.flow.Flow

interface ProfileSettingsRepository {
    fun observeSettings(): Flow<ProfileSettings>

    suspend fun updateAppearance(appearance: ProfileAppearance)

    suspend fun updateLanguage(language: ProfileLanguage)

    suspend fun updateNotificationPreferences(isPushEnabled: Boolean, isEmailEnabled: Boolean, isInAppEnabled: Boolean)

    /** Brings the device copy in line with the signed-in account; local-only settings do nothing. */
    suspend fun syncFromServer() = Unit
}
