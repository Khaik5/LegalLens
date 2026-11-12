package com.example.lagallens.data.datasource.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.lagallens.domain.model.ProfileAppearance
import com.example.lagallens.domain.model.ProfileLanguage
import com.example.lagallens.domain.model.ProfileSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.profileSettingsDataStore by preferencesDataStore(name = "profile_settings")

/** Device copy of the settings, plus which account still has changes the backend has not received. */
interface ProfileSettingsStore {
    fun observeSettings(): Flow<ProfileSettings>
    suspend fun updateAppearance(appearance: ProfileAppearance)
    suspend fun updateLanguage(language: ProfileLanguage)
    suspend fun updateNotificationPreferences(isPushEnabled: Boolean, isEmailEnabled: Boolean, isInAppEnabled: Boolean)
    suspend fun replace(settings: ProfileSettings)
    suspend fun pendingOwner(): String?
    suspend fun markPending(ownerId: String)
    suspend fun clearPending()
}

class ProfileSettingsLocalDataSource(
    private val context: Context
) : ProfileSettingsStore {
    override fun observeSettings(): Flow<ProfileSettings> {
        return context.profileSettingsDataStore.data.map { preferences ->
            ProfileSettings(
                appearance = preferences[APPEARANCE_KEY].toAppearance(),
                language = preferences[LANGUAGE_KEY].toLanguage(),
                isPushEnabled = preferences[PUSH_KEY] ?: true,
                isEmailEnabled = preferences[EMAIL_KEY] ?: true,
                isInAppEnabled = preferences[IN_APP_KEY] ?: false
            )
        }
    }

    override suspend fun updateAppearance(appearance: ProfileAppearance) {
        context.profileSettingsDataStore.edit { preferences ->
            preferences[APPEARANCE_KEY] = appearance.name
        }
    }

    override suspend fun updateLanguage(language: ProfileLanguage) {
        context.profileSettingsDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.name
        }
    }

    override suspend fun updateNotificationPreferences(isPushEnabled: Boolean, isEmailEnabled: Boolean, isInAppEnabled: Boolean) {
        context.profileSettingsDataStore.edit { preferences ->
            preferences[PUSH_KEY] = isPushEnabled
            preferences[EMAIL_KEY] = isEmailEnabled
            preferences[IN_APP_KEY] = isInAppEnabled
        }
    }

    override suspend fun replace(settings: ProfileSettings) {
        context.profileSettingsDataStore.edit { preferences ->
            preferences[APPEARANCE_KEY] = settings.appearance.name
            preferences[LANGUAGE_KEY] = settings.language.name
            preferences[PUSH_KEY] = settings.isPushEnabled
            preferences[EMAIL_KEY] = settings.isEmailEnabled
            preferences[IN_APP_KEY] = settings.isInAppEnabled
        }
    }

    override suspend fun pendingOwner(): String? = context.profileSettingsDataStore.data.first()[PENDING_OWNER_KEY]

    override suspend fun markPending(ownerId: String) {
        context.profileSettingsDataStore.edit { it[PENDING_OWNER_KEY] = ownerId }
    }

    override suspend fun clearPending() {
        context.profileSettingsDataStore.edit { it.remove(PENDING_OWNER_KEY) }
    }

    private fun String?.toAppearance(): ProfileAppearance {
        return ProfileAppearance.entries.find { it.name == this } ?: ProfileAppearance.SYSTEM
    }

    private fun String?.toLanguage(): ProfileLanguage {
        return profileLanguageOrDefault(this)
    }

    private companion object {
        val APPEARANCE_KEY: Preferences.Key<String> = stringPreferencesKey("appearance")
        val LANGUAGE_KEY: Preferences.Key<String> = stringPreferencesKey("language")
        val PUSH_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("notification_push")
        val EMAIL_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("notification_email")
        val IN_APP_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("notification_in_app")
        val PENDING_OWNER_KEY: Preferences.Key<String> = stringPreferencesKey("pending_sync_user_id")
    }
}

internal fun profileLanguageOrDefault(value: String?): ProfileLanguage {
    return ProfileLanguage.entries.find { it.name == value } ?: ProfileLanguage.VIETNAMESE
}
