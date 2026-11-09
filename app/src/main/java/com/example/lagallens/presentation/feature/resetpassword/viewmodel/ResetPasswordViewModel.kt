package com.example.lagallens.presentation.feature.resetpassword.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.usecase.ConfirmPasswordResetUseCase
import com.example.lagallens.presentation.feature.resetpassword.contract.ResetPasswordUiEffect
import com.example.lagallens.presentation.feature.resetpassword.contract.ResetPasswordUiEvent
import com.example.lagallens.presentation.feature.resetpassword.contract.ResetPasswordUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResetPasswordViewModel(private val confirmPasswordReset: ConfirmPasswordResetUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<ResetPasswordUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: ResetPasswordUiEvent) {
        when (event) {
            is ResetPasswordUiEvent.Initialized -> _uiState.update { it.copy(email = event.email) }
            is ResetPasswordUiEvent.PasswordChanged -> _uiState.update { it.copy(password = event.value, passwordErrorResId = null) }
            is ResetPasswordUiEvent.ConfirmPasswordChanged -> _uiState.update {
                it.copy(confirmPassword = event.value, confirmPasswordErrorResId = null)
            }
            ResetPasswordUiEvent.BackClicked -> _uiEffect.trySend(ResetPasswordUiEffect.CloseScreen)
            ResetPasswordUiEvent.SubmitClicked -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return
        val passwordError = when {
            state.password.isEmpty() -> R.string.register_password_required
            state.password.length < MINIMUM_PASSWORD_LENGTH -> R.string.register_password_minimum_length
            else -> null
        }
        val confirmationError = when {
            state.confirmPassword.isEmpty() -> R.string.register_confirm_password_required
            state.confirmPassword != state.password -> R.string.register_password_mismatch
            else -> null
        }
        if (passwordError != null || confirmationError != null) {
            _uiState.update { it.copy(passwordErrorResId = passwordError, confirmPasswordErrorResId = confirmationError) }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = try {
                confirmPasswordReset(state.email, state.password)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                AuthResult.REJECTED
            }
            _uiState.update {
                it.copy(isLoading = false, passwordErrorResId = R.string.reset_password_failed.takeIf { result != AuthResult.SUCCESS })
            }
            if (result == AuthResult.SUCCESS) _uiEffect.send(ResetPasswordUiEffect.NavigateToLogin)
        }
    }

    class Factory(private val confirmPasswordReset: ConfirmPasswordResetUseCase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ResetPasswordViewModel(confirmPasswordReset) as T
    }

    private companion object {
        const val MINIMUM_PASSWORD_LENGTH = 6
    }
}
