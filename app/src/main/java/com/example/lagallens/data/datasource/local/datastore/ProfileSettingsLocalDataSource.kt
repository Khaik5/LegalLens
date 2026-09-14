package com.example.lagallens.data.datasource.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.lagallens.domain.model.ProfileAppearance
import com.example.lagallens.domain.model.ProfileLanguage
import com.example.lagallens.domain.model.ProfileSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileSettingsDataStore by preferencesDataStore(name = "profile_settings")

class ProfileSettingsLocalDataSource(
    private val context: Context
) {
    fun observeSettings(): Flow<ProfileSettings> {
        return context.profileSettingsDataStore.data.map { preferences ->
            ProfileSettings(
                appearance = preferences[APPEARANCE_KEY].toAppearance(),
                language = preferences[LANGUAGE_KEY].toLanguage()
            )
        }
    }

    suspend fun updateAppearance(appearance: ProfileAppearance) {
        context.profileSettingsDataStore.edit { preferences ->
            preferences[APPEARANCE_KEY] = appearance.name
        }
    }

    suspend fun updateLanguage(language: ProfileLanguage) {
        context.profileSettingsDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.name
        }
    }

    private fun String?.toAppearance(): ProfileAppearance {
        return ProfileAppearance.entries.find { it.name == this } ?: ProfileAppearance.SYSTEM
    }

    private fun String?.toLanguage(): ProfileLanguage {
        return ProfileLanguage.entries.find { it.name == this } ?: ProfileLanguage.VIETNAMESE
    }

    private companion object {
        val APPEARANCE_KEY: Preferences.Key<String> = stringPreferencesKey("appearance")
        val LANGUAGE_KEY: Preferences.Key<String> = stringPreferencesKey("language")
    }
}
