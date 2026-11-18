package com.example.lagallens.presentation

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.model.ProfileSettings
import com.example.lagallens.domain.repository.OnboardingRepository
import com.example.lagallens.domain.repository.ProfileSettingsRepository
import com.example.lagallens.domain.usecase.*
import com.example.lagallens.presentation.feature.onboarding.viewmodel.OnboardingViewModel
import com.example.lagallens.presentation.feature.onboarding.contract.*
import com.example.lagallens.presentation.feature.splash.viewmodel.SplashViewModel
import com.example.lagallens.presentation.feature.splash.contract.*
import com.example.lagallens.presentation.feature.profile.settings.viewmodel.ProfileSettingsViewModel
import com.example.lagallens.presentation.feature.profile.settings.contract.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StorageFailureRegressionTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun onboardingAllowsAnotherAttemptAfterWriteFailure() = runTest {
        var calls = 0
        val repository = object : OnboardingRepository {
            override suspend fun isCompleted() = false
            override suspend fun markCompleted() { if (++calls == 1) throw IOException("disk") }
        }
        val model = OnboardingViewModel(CompleteOnboardingUseCase(repository))
        model.onEvent(OnboardingUiEvent.SkipClicked)
        runCurrent()
        model.onEvent(OnboardingUiEvent.SkipClicked)
        runCurrent()
        assertEquals(2, calls)
        // The failed first attempt tells the user; the second one goes on.
        assertEquals(
            listOf(OnboardingUiEffect.ShowSaveFailed, OnboardingUiEffect.NavigateToHome),
            model.uiEffect.take(2).toList()
        )
    }

    @Test fun settingsWriteFailuresProduceFeedbackAndKeepPersistedValues() = runTest {
        val repository = object : ProfileSettingsRepository {
            override fun observeSettings() = flowOf(ProfileSettings())
            override suspend fun updateAppearance(appearance: com.example.lagallens.domain.model.ProfileAppearance): Unit = throw IOException("disk")
            override suspend fun updateLanguage(language: com.example.lagallens.domain.model.ProfileLanguage): Unit = throw IOException("disk")
            override suspend fun updateNotificationPreferences(isPushEnabled: Boolean, isEmailEnabled: Boolean, isInAppEnabled: Boolean): Unit = throw IOException("disk")
        }
        val model = ProfileSettingsViewModel(ObserveProfileSettingsUseCase(repository), UpdateProfileAppearanceUseCase(repository), UpdateProfileLanguageUseCase(repository), UpdateNotificationPreferencesUseCase(repository))
        model.onEvent(ProfileSettingsUiEvent.AppearanceSelected(ProfileAppearance.LIGHT))
        runCurrent()
        assertEquals(ProfileAppearance.SYSTEM, model.uiState.value.appearance)
        assertNotNull(model.uiEffect.first())
        model.onEvent(ProfileSettingsUiEvent.LanguageSelected(ProfileLanguage.ENGLISH))
        runCurrent()
        assertEquals(ProfileLanguage.VIETNAMESE, model.uiState.value.language)
        assertNotNull(model.uiEffect.first())
        model.onEvent(ProfileSettingsUiEvent.PushToggled(false))
        runCurrent()
        assertTrue(model.uiState.value.isPushEnabled)
        assertNotNull(model.uiEffect.first())
    }
}
