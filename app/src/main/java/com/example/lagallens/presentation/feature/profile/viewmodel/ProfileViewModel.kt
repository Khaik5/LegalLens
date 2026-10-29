package com.example.lagallens.presentation.feature.profile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.presentation.feature.profile.contract.ProfileUiEffect
import com.example.lagallens.presentation.feature.profile.contract.ProfileUiEvent
import com.example.lagallens.presentation.feature.profile.contract.ProfileUiState
import com.example.lagallens.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val profileRepository = (application as LegalLensApplication).userProfileRepository
    private val initialProfileResult = runCatching { profileRepository.getProfile() }
    private val initialProfile = initialProfileResult.getOrElse { UserProfile(name = "", email = "") }
    private val _uiState = MutableStateFlow(
        ProfileUiState(
            name = initialProfile.name,
            email = initialProfile.email,
            phone = initialProfile.phone,
            photoUri = initialProfile.photoUri,
            isBackendUnavailable = initialProfileResult.isFailure
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProfileUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: ProfileUiEvent) {
        when (event) {
            is ProfileUiEvent.ActionClicked -> emitEffect(
                ProfileUiEffect.ShowActionUnavailable(event.actionTitleRes)
            )
            ProfileUiEvent.EditProfileClicked -> emitEffect(ProfileUiEffect.NavigateToEditProfile)
            ProfileUiEvent.SettingsClicked -> emitEffect(
                ProfileUiEffect.NavigateToSettings
            )
            ProfileUiEvent.ChangePasswordClicked -> emitEffect(ProfileUiEffect.NavigateToChangePassword)
            ProfileUiEvent.PrivacyClicked -> emitEffect(ProfileUiEffect.NavigateToPrivacy)
            ProfileUiEvent.AboutClicked -> emitEffect(ProfileUiEffect.NavigateToAbout)
            is ProfileUiEvent.ProfileUpdated -> runCatching {
                profileRepository.saveProfile(event.name, event.phone, event.photoUri)
            }.onSuccess { profile ->
                _uiState.update { it.copy(name = profile.name, email = profile.email, phone = profile.phone, photoUri = profile.photoUri, isBackendUnavailable = false) }
            }.onFailure {
                _uiState.update { state -> state.copy(isBackendUnavailable = true) }
            }
            ProfileUiEvent.LogoutClicked -> emitEffect(ProfileUiEffect.ShowLogoutConfirmation)
            ProfileUiEvent.LogoutConfirmed -> emitEffect(ProfileUiEffect.NavigateToAuthentication)
        }
    }

    private fun emitEffect(effect: ProfileUiEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }
}
