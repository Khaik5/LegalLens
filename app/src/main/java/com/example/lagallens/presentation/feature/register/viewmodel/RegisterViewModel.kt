package com.example.lagallens.presentation.feature.register.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.lagallens.presentation.feature.register.contract.RegisterUiEffect
import com.example.lagallens.presentation.feature.register.contract.RegisterUiEvent
import com.example.lagallens.presentation.feature.register.contract.RegisterUiState

class RegisterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<RegisterUiEffect>()
    val uiEffect: SharedFlow<RegisterUiEffect> = _uiEffect.asSharedFlow()

    fun onEvent(event: RegisterUiEvent) {
        when (event) {
            RegisterUiEvent.BackClicked -> sendEffect(RegisterUiEffect.CloseScreen)
            is RegisterUiEvent.FullNameChanged -> _uiState.update { it.copy(fullName = event.value, fullNameErrorResId = null) }
            is RegisterUiEvent.EmailChanged -> _uiState.update { it.copy(email = event.value, emailErrorResId = null) }
            is RegisterUiEvent.PasswordChanged -> _uiState.update { it.copy(password = event.value, passwordErrorResId = null) }
            is RegisterUiEvent.ConfirmPasswordChanged -> _uiState.update {
                it.copy(confirmPassword = event.value, confirmPasswordErrorResId = null)
            }
            RegisterUiEvent.LoginClicked -> sendEffect(RegisterUiEffect.NavigateToLogin)
            RegisterUiEvent.SubmitClicked -> register()
        }
    }

    private fun register() {
        val state = _uiState.value
        val fullNameErrorResId = if (state.fullName.trim().isEmpty()) R.string.register_full_name_required else null
        val emailErrorResId = if (!EMAIL_PATTERN.matches(state.email.trim())) R.string.register_email_invalid else null
        val passwordErrorResId = when {
            state.password.isEmpty() -> R.string.register_password_required
            state.password.length < MINIMUM_PASSWORD_LENGTH -> R.string.register_password_minimum_length
            else -> null
        }
        val confirmPasswordErrorResId = when {
            state.confirmPassword.isEmpty() -> R.string.register_confirm_password_required
            state.confirmPassword != state.password -> R.string.register_password_mismatch
            else -> null
        }
        if (listOf(fullNameErrorResId, emailErrorResId, passwordErrorResId, confirmPasswordErrorResId).any { it != null }) {
            _uiState.update {
                it.copy(
                    fullNameErrorResId = fullNameErrorResId,
                    emailErrorResId = emailErrorResId,
                    passwordErrorResId = passwordErrorResId,
                    confirmPasswordErrorResId = confirmPasswordErrorResId
                )
            }
            return
        }
        sendEffect(RegisterUiEffect.NavigateToOtp(state.email.trim()))
    }

    private fun sendEffect(effect: RegisterUiEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }

    private companion object {
        const val MINIMUM_PASSWORD_LENGTH = 6
        val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    }
}
