package com.example.lagallens.presentation.feature.profile.password.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.profile.password.contract.ProfilePasswordUiEffect
import com.example.lagallens.presentation.feature.profile.password.contract.ProfilePasswordUiEvent
import com.example.lagallens.presentation.feature.profile.password.contract.ProfilePasswordUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.lagallens.LegalLensApplication

class ProfilePasswordViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ProfilePasswordUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProfilePasswordUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: ProfilePasswordUiEvent) {
        when (event) {
            is ProfilePasswordUiEvent.CurrentPasswordChanged -> _uiState.update {
                it.copy(currentPassword = event.value, currentPasswordErrorRes = null)
            }

            is ProfilePasswordUiEvent.NewPasswordChanged -> _uiState.update {
                it.copy(newPassword = event.value, newPasswordErrorRes = null)
            }

            is ProfilePasswordUiEvent.ConfirmationChanged -> _uiState.update {
                it.copy(confirmation = event.value, confirmationErrorRes = null)
            }

            ProfilePasswordUiEvent.SubmitClicked -> submitPasswordChange()
            ProfilePasswordUiEvent.BackClicked -> emitEffect(ProfilePasswordUiEffect.NavigateBack)
        }
    }

    private fun submitPasswordChange() {
        val state = _uiState.value
        val currentPasswordError = if (state.currentPassword.isBlank()) {
            R.string.profile_password_current_required
        } else {
            null
        }
        val newPasswordError = when {
            !state.meetsSecurityRequirements -> R.string.profile_password_requirements_not_met
            state.newPassword == state.currentPassword -> R.string.profile_password_same_as_current
            else -> null
        }
        val confirmationError = if (state.confirmation != state.newPassword) {
            R.string.profile_password_confirmation_mismatch
        } else {
            null
        }

        if (currentPasswordError != null || newPasswordError != null || confirmationError != null) {
            _uiState.update {
                it.copy(
                    currentPasswordErrorRes = currentPasswordError,
                    newPasswordErrorRes = newPasswordError,
                    confirmationErrorRes = confirmationError
                )
            }
            return
        }

        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val app = getApplication<Application>() as LegalLensApplication
            val result = runCatching { withContext(Dispatchers.IO) { app.changeCurrentDemoPassword(state.currentPassword, state.newPassword) } }
            when {
                result.isFailure -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    emitEffect(ProfilePasswordUiEffect.ShowBackendUnavailable)
                }
                result.getOrDefault(false) -> {
                    _uiState.value = ProfilePasswordUiState()
                    emitEffect(ProfilePasswordUiEffect.PasswordUpdated)
                }
                else -> {
                    _uiState.update { it.copy(isSubmitting = false, currentPasswordErrorRes = R.string.profile_password_current_incorrect) }
                    emitEffect(ProfilePasswordUiEffect.CurrentPasswordIncorrect)
                }
            }
        }
    }

    private fun emitEffect(effect: ProfilePasswordUiEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }
}
