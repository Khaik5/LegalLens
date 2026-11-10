package com.example.lagallens.presentation.feature.register.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.model.auth.AuthFailure
import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.model.auth.OtpPurpose
import com.example.lagallens.domain.usecase.ResendOtpUseCase
import com.example.lagallens.presentation.common.utils.toAuthMessage
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

class RegisterViewModel(
    private val registerAccount: RegisterAccountUseCase,
    private val resendOtp: ResendOtpUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<RegisterUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: RegisterUiEvent) {
        when (event) {
            RegisterUiEvent.BackClicked -> sendEffect(RegisterUiEffect.CloseScreen)
            is RegisterUiEvent.FullNameChanged -> _uiState.update { it.copy(fullName = event.value, fullNameErrorResId = null) }
            is RegisterUiEvent.EmailChanged -> _uiState.update { it.copy(email = event.value, emailErrorResId = null, canResendVerification = false) }
            is RegisterUiEvent.PasswordChanged -> _uiState.update { it.copy(password = event.value, passwordErrorResId = null) }
            is RegisterUiEvent.ConfirmPasswordChanged -> _uiState.update {
                it.copy(confirmPassword = event.value, confirmPasswordErrorResId = null)
            }
            RegisterUiEvent.LoginClicked -> sendEffect(RegisterUiEffect.NavigateToLogin)
            RegisterUiEvent.SubmitClicked -> register()
            RegisterUiEvent.ResendVerificationClicked -> resendVerification()
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
        val email = state.email.trim()
        _uiState.update { it.copy(isLoading = true, canResendVerification = false, emailErrorResId = null, passwordErrorResId = null) }
        viewModelScope.launch {
            val result = try {
                registerAccount(state.fullName.trim(), email, state.password)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                AuthResult.Failed(AuthFailure.REJECTED)
            }
            when (result) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    val challenge = result.value
                    _uiEffect.send(
                        if (challenge == null) RegisterUiEffect.NavigateToLogin
                        else RegisterUiEffect.NavigateToOtp(email, challenge)
                    )
                }
                is AuthResult.Failed -> _uiState.update {
                    val emailTaken = result.reason == AuthFailure.EMAIL_TAKEN && it.email.trim() == email
                    it.copy(
                        isLoading = false,
                        emailErrorResId = R.string.register_email_taken.takeIf { emailTaken },
                        passwordErrorResId = result.reason.toAuthMessage().takeUnless { result.reason == AuthFailure.EMAIL_TAKEN },
                        canResendVerification = emailTaken
                    )
                }
            }
        }
    }

    private fun resendVerification() {
        val state = _uiState.value
        if (state.isLoading || !state.canResendVerification) return
        val email = state.email.trim()
        _uiState.update { it.copy(isLoading = true, emailErrorResId = null) }
        viewModelScope.launch {
            val result = try {
                resendOtp(email, OtpPurpose.REGISTRATION)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                AuthResult.Failed(AuthFailure.REJECTED)
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    emailErrorResId = (result as? AuthResult.Failed)?.reason?.toAuthMessage()
                        ?.takeIf { _ -> it.email.trim() == email }
                )
            }
            if (result is AuthResult.Success) _uiEffect.send(RegisterUiEffect.NavigateToOtp(email, result.value))
        }
    }

    private fun sendEffect(effect: RegisterUiEffect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }

    class Factory(
        private val registerAccount: RegisterAccountUseCase,
        private val resendOtp: ResendOtpUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RegisterViewModel(registerAccount, resendOtp) as T
    }

    private companion object {
        const val MINIMUM_PASSWORD_LENGTH = 8
        val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    }
}
