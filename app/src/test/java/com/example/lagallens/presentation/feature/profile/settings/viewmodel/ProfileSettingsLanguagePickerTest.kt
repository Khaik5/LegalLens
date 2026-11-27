package com.example.lagallens.presentation.feature.profile.settings.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.model.ProfileSettings
import com.example.lagallens.domain.repository.ProfileSettingsRepository
import com.example.lagallens.domain.usecase.ObserveProfileSettingsUseCase
import com.example.lagallens.domain.usecase.UpdateNotificationPreferencesUseCase
import com.example.lagallens.domain.usecase.UpdateProfileAppearanceUseCase
import com.example.lagallens.domain.usecase.UpdateProfileLanguageUseCase
import com.example.lagallens.presentation.feature.profile.settings.contract.ProfileLanguage
import com.example.lagallens.presentation.feature.profile.settings.contract.ProfileSettingsUiEffect
import com.example.lagallens.presentation.feature.profile.settings.contract.ProfileSettingsUiEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProfileSettingsLanguagePickerTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun pickerOpenedBeforeSettingsLoadShowsTheSavedLanguage() = runTest {
        val settings = MutableSharedFlow<ProfileSettings>(replay = 1)
        val repository = object : ProfileSettingsRepository {
            override fun observeSettings() = settings
            override suspend fun updateAppearance(appearance: com.example.lagallens.domain.model.ProfileAppearance) = Unit
            override suspend fun updateLanguage(language: com.example.lagallens.domain.model.ProfileLanguage) = Unit
            override suspend fun updateNotificationPreferences(isPushEnabled: Boolean, isEmailEnabled: Boolean, isInAppEnabled: Boolean) = Unit
        }
        val model = ProfileSettingsViewModel(
            ObserveProfileSettingsUseCase(repository),
            UpdateProfileAppearanceUseCase(repository),
            UpdateProfileLanguageUseCase(repository),
            UpdateNotificationPreferencesUseCase(repository)
        )
        val effects = mutableListOf<ProfileSettingsUiEffect>()
        val collector = launch { model.uiEffect.collect { effects += it } }

        model.onEvent(ProfileSettingsUiEvent.LanguageClicked)
        runCurrent()
        assertTrue("The picker must wait for the saved language", effects.isEmpty())

        settings.emit(ProfileSettings(language = com.example.lagallens.domain.model.ProfileLanguage.ENGLISH))
        runCurrent()
        assertEquals(listOf(ProfileSettingsUiEffect.ShowLanguagePicker(ProfileLanguage.ENGLISH)), effects)

        model.onEvent(ProfileSettingsUiEvent.LanguageClicked)
        runCurrent()
        assertEquals(ProfileSettingsUiEffect.ShowLanguagePicker(ProfileLanguage.ENGLISH), effects.last())
        collector.cancel()
    }
}
