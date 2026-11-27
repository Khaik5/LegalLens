package com.example.lagallens.presentation.feature.profile.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.usecase.ObserveProfileSettingsUseCase
import com.example.lagallens.domain.usecase.UpdateProfileAppearanceUseCase
import com.example.lagallens.domain.usecase.UpdateProfileLanguageUseCase
import com.example.lagallens.presentation.feature.profile.settings.contract.ProfileAppearance
import com.example.lagallens.presentation.feature.profile.settings.contract.ProfileLanguage
import com.example.lagallens.presentation.feature.profile.settings.contract.ProfileSettingsUiEffect
import com.example.lagallens.presentation.feature.profile.settings.contract.ProfileSettingsUiEvent
import com.example.lagallens.presentation.feature.profile.settings.contract.ProfileSettingsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

class ProfileSettingsViewModel(
    observeProfileSettingsUseCase: ObserveProfileSettingsUseCase,
    private val updateProfileAppearanceUseCase: UpdateProfileAppearanceUseCase,
    private val updateProfileLanguageUseCase: UpdateProfileLanguageUseCase,
    private val updateNotificationPreferencesUseCase: com.example.lagallens.domain.usecase.UpdateNotificationPreferencesUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileSettingsUiState())
    val uiState = _uiState.asStateFlow()
    private var persistedState = _uiState.value
    private var hasLoadedSettings = false
    private var isLanguagePickerPending = false

    private val _uiEffect = Channel<ProfileSettingsUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeProfileSettingsUseCase().collect { settings ->
                _uiState.update {
                    it.copy(
                        appearance = profileAppearanceFrom(settings.appearance.name),
                        language = profileLanguageFrom(settings.language.name),
                        isPushEnabled = settings.isPushEnabled,
                        isEmailEnabled = settings.isEmailEnabled,
                        isInAppEnabled = settings.isInAppEnabled
                    )
                }
                persistedState = _uiState.value
                hasLoadedSettings = true
                // Opened straight from the profile, the picker must wait for the saved language or it marks the default.
                if (isLanguagePickerPending) {
                    isLanguagePickerPending = false
                    _uiEffect.send(ProfileSettingsUiEffect.ShowLanguagePicker(_uiState.value.language))
                }
            }
        }
    }

    fun onEvent(event: ProfileSettingsUiEvent) {
        when (event) {
            is ProfileSettingsUiEvent.PushToggled -> saveNotifications { it.copy(isPushEnabled = event.isEnabled) }
            is ProfileSettingsUiEvent.EmailToggled -> saveNotifications { it.copy(isEmailEnabled = event.isEnabled) }
            is ProfileSettingsUiEvent.InAppToggled -> saveNotifications { it.copy(isInAppEnabled = event.isEnabled) }
            is ProfileSettingsUiEvent.AppearanceSelected -> updateAppearance(event.appearance)
            ProfileSettingsUiEvent.LanguageClicked -> if (hasLoadedSettings) {
                emitEffect(ProfileSettingsUiEffect.ShowLanguagePicker(_uiState.value.language))
            } else {
                isLanguagePickerPending = true
            }
            is ProfileSettingsUiEvent.LanguageSelected -> saveSetting {
                updateProfileLanguageUseCase(
                    com.example.lagallens.domain.model.ProfileLanguage.valueOf(event.language.name)
                )
            }
            ProfileSettingsUiEvent.BackClicked -> emitEffect(ProfileSettingsUiEffect.NavigateBack)
        }
    }

    private fun saveNotifications(change: (ProfileSettingsUiState) -> ProfileSettingsUiState) {
        val previous = _uiState.value
        val updated = change(previous)
        if (updated == previous) return
        _uiState.value = updated
        saveSetting {
            updateNotificationPreferencesUseCase(updated.isPushEnabled, updated.isEmailEnabled, updated.isInAppEnabled)
        }
    }

    private fun saveSetting(write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
            } catch (_: IOException) {
                _uiState.value = persistedState
                _uiEffect.send(ProfileSettingsUiEffect.ShowSaveFailed)
            }
        }
    }

    private fun emitEffect(effect: ProfileSettingsUiEffect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }

    private fun updateAppearance(appearance: ProfileAppearance) {
        _uiState.update { it.copy(appearance = appearance) }
        saveSetting {
            updateProfileAppearanceUseCase(
                com.example.lagallens.domain.model.ProfileAppearance.valueOf(appearance.name)
            )
        }
    }

    private fun profileAppearanceFrom(value: String): ProfileAppearance {
        return ProfileAppearance.entries.find { it.name == value } ?: ProfileAppearance.SYSTEM
    }

    private fun profileLanguageFrom(value: String): ProfileLanguage {
        return ProfileLanguage.entries.find { it.name == value } ?: ProfileLanguage.VIETNAMESE
    }
}
