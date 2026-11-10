package com.example.lagallens.presentation.feature.profile.password.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.model.auth.ChangePasswordResult
import com.example.lagallens.domain.usecase.ChangePasswordUseCase
import kotlinx.coroutines.CancellationException
import com.example.lagallens.presentation.feature.profile.password.contract.ProfilePasswordUiEffect
import com.example.lagallens.presentation.feature.profile.password.contract.ProfilePasswordUiEvent
import com.example.lagallens.presentation.feature.profile.password.contract.ProfilePasswordUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfilePasswordViewModel(private val changePassword: ChangePasswordUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfilePasswordUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<ProfilePasswordUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

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
            val result = try {
                changePassword(state.currentPassword, state.newPassword)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                ChangePasswordResult.Failed
            }
            when (result) {
                ChangePasswordResult.UpdatedSignInRequired -> {
                    _uiState.value = ProfilePasswordUiState(isSubmitting = true)
                    _uiEffect.send(ProfilePasswordUiEffect.NavigateToLogin)
                }
                ChangePasswordResult.Updated -> {
                    _uiState.value = ProfilePasswordUiState()
                    _uiEffect.send(ProfilePasswordUiEffect.PasswordUpdated)
                }
                ChangePasswordResult.IncorrectCurrentPassword -> _uiState.update {
                    it.copy(isSubmitting = false, currentPasswordErrorRes = R.string.profile_password_current_incorrect)
                }
                ChangePasswordResult.RateLimited, ChangePasswordResult.Failed -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    val message = if (result == ChangePasswordResult.RateLimited) {
                        R.string.profile_password_rate_limited
                    } else {
                        R.string.profile_password_backend_unavailable
                    }
                    _uiEffect.send(ProfilePasswordUiEffect.ShowError(message))
                }
            }
        }
    }

    class Factory(private val changePassword: ChangePasswordUseCase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ProfilePasswordViewModel(changePassword) as T
    }

    private fun emitEffect(effect: ProfilePasswordUiEffect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }
}
