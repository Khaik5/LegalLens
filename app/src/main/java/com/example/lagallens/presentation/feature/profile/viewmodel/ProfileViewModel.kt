package com.example.lagallens.presentation.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.profile.contract.ProfileUiEffect
import com.example.lagallens.presentation.feature.profile.contract.ProfileUiEvent
import com.example.lagallens.presentation.feature.profile.contract.ProfileUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProfileUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: ProfileUiEvent) {
        when (event) {
            is ProfileUiEvent.ActionClicked -> emitEffect(
                ProfileUiEffect.ShowActionUnavailable(event.actionTitleRes)
            )
            ProfileUiEvent.EditProfileClicked -> emitEffect(ProfileUiEffect.NavigateToEditProfile)
            ProfileUiEvent.NotificationSettingsClicked -> emitEffect(
                ProfileUiEffect.NavigateToNotificationSettings
            )
            ProfileUiEvent.ChangePasswordClicked -> emitEffect(ProfileUiEffect.NavigateToChangePassword)
            is ProfileUiEvent.ProfileUpdated -> _uiState.update {
                it.copy(name = event.name, phone = event.phone)
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


