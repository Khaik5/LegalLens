package com.example.lagallens.presentation.feature.register.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.usecase.RegisterAccountUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.lagallens.presentation.feature.register.contract.RegisterUiEffect
import com.example.lagallens.presentation.feature.register.contract.RegisterUiEvent
import com.example.lagallens.presentation.feature.register.contract.RegisterUiState

class RegisterViewModel(private val registerAccount: RegisterAccountUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<RegisterUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

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
        if (state.isLoading) return
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
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = try {
                registerAccount(state.fullName.trim(), state.email.trim(), state.password)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                AuthResult.REJECTED
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    emailErrorResId = R.string.register_email_taken.takeIf { result == AuthResult.EMAIL_TAKEN },
                    passwordErrorResId = R.string.auth_backend_not_configured.takeIf { result == AuthResult.REJECTED }
                )
            }
            if (result == AuthResult.SUCCESS) sendEffect(RegisterUiEffect.NavigateToOtp(state.email.trim()))
        }
    }

    private fun sendEffect(effect: RegisterUiEffect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }

    class Factory(private val registerAccount: RegisterAccountUseCase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RegisterViewModel(registerAccount) as T
    }

    private companion object {
        const val MINIMUM_PASSWORD_LENGTH = 6
        val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    }
}
