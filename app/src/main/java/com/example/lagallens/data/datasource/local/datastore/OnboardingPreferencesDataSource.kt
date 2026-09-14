package com.example.lagallens.data.datasource.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_preferences")

class OnboardingPreferencesDataSource(
    private val context: Context
) {
    suspend fun isCompleted(): Boolean =
        context.onboardingDataStore.data.first()[ONBOARDING_COMPLETED] ?: false

    suspend fun markCompleted() {
        context.onboardingDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = true
        }
    }

    private companion object {
        val ONBOARDING_COMPLETED: Preferences.Key<Boolean> =
            booleanPreferencesKey("onboarding_completed")
    }
}
