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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileSettingsViewModel(
    observeProfileSettingsUseCase: ObserveProfileSettingsUseCase,
    private val updateProfileAppearanceUseCase: UpdateProfileAppearanceUseCase,
    private val updateProfileLanguageUseCase: UpdateProfileLanguageUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileSettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProfileSettingsUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            observeProfileSettingsUseCase().collect { settings ->
                _uiState.update {
                    it.copy(
                        appearance = profileAppearanceFrom(settings.appearance.name),
                        language = profileLanguageFrom(settings.language.name)
                    )
                }
            }
        }
    }

    fun onEvent(event: ProfileSettingsUiEvent) {
        when (event) {
            is ProfileSettingsUiEvent.PushToggled -> _uiState.update {
                it.copy(isPushEnabled = event.isEnabled)
            }
            is ProfileSettingsUiEvent.EmailToggled -> _uiState.update {
                it.copy(isEmailEnabled = event.isEnabled)
            }
            is ProfileSettingsUiEvent.InAppToggled -> _uiState.update {
                it.copy(isInAppEnabled = event.isEnabled)
            }
            is ProfileSettingsUiEvent.BiometricToggled -> _uiState.update {
                it.copy(isBiometricEnabled = event.isEnabled)
            }
            is ProfileSettingsUiEvent.AppearanceSelected -> viewModelScope.launch {
                updateProfileAppearanceUseCase(
                    com.example.lagallens.domain.model.ProfileAppearance.valueOf(event.appearance.name)
                )
            }
            ProfileSettingsUiEvent.LanguageClicked -> emitEffect(
                ProfileSettingsUiEffect.ShowLanguagePicker(_uiState.value.language)
            )
            is ProfileSettingsUiEvent.LanguageSelected -> viewModelScope.launch {
                updateProfileLanguageUseCase(
                    com.example.lagallens.domain.model.ProfileLanguage.valueOf(event.language.name)
                )
            }
            ProfileSettingsUiEvent.BackClicked -> emitEffect(ProfileSettingsUiEffect.NavigateBack)
        }
    }

    private fun emitEffect(effect: ProfileSettingsUiEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }

    private fun profileAppearanceFrom(value: String): ProfileAppearance {
        return ProfileAppearance.entries.find { it.name == value } ?: ProfileAppearance.SYSTEM
    }

    private fun profileLanguageFrom(value: String): ProfileLanguage {
        return ProfileLanguage.entries.find { it.name == value } ?: ProfileLanguage.VIETNAMESE
    }
}
