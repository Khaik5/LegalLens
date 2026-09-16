package com.example.lagallens.presentation.feature.register.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            is RegisterUiEvent.FullNameChanged -> _uiState.update { it.copy(fullName = event.value, fullNameError = null) }
            is RegisterUiEvent.EmailChanged -> _uiState.update { it.copy(email = event.value, emailError = null) }
            is RegisterUiEvent.PasswordChanged -> _uiState.update { it.copy(password = event.value, passwordError = null) }
            is RegisterUiEvent.ConfirmPasswordChanged -> _uiState.update {
                it.copy(confirmPassword = event.value, confirmPasswordError = null)
            }
            RegisterUiEvent.LoginClicked -> sendEffect(RegisterUiEffect.NavigateToLogin)
            RegisterUiEvent.SubmitClicked -> register()
        }
    }

    private fun register() {
        val state = _uiState.value
        val fullNameError = if (state.fullName.trim().isEmpty()) "Vui lòng nhập họ và tên" else null
        val emailError = if (!EMAIL_PATTERN.matches(state.email.trim())) "Vui lòng nhập email hợp lệ" else null
        val passwordError = when {
            state.password.isEmpty() -> "Vui lòng nhập mật khẩu"
            state.password.length < MINIMUM_PASSWORD_LENGTH -> "Mật khẩu cần ít nhất 6 ký tự"
            else -> null
        }
        val confirmPasswordError = when {
            state.confirmPassword.isEmpty() -> "Vui lòng xác nhận mật khẩu"
            state.confirmPassword != state.password -> "Mật khẩu xác nhận không khớp"
            else -> null
        }
        if (listOf(fullNameError, emailError, passwordError, confirmPasswordError).any { it != null }) {
            _uiState.update {
                it.copy(
                    fullNameError = fullNameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError
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
