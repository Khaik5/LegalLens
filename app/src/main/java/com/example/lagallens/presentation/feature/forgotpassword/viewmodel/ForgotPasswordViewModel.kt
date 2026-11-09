package com.example.lagallens.presentation.feature.forgotpassword.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.usecase.RequestPasswordResetUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.lagallens.presentation.feature.forgotpassword.contract.ForgotPasswordUiEffect
import com.example.lagallens.presentation.feature.forgotpassword.contract.ForgotPasswordUiEvent
import com.example.lagallens.presentation.feature.forgotpassword.contract.ForgotPasswordUiState

class ForgotPasswordViewModel(private val requestPasswordReset: RequestPasswordResetUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<ForgotPasswordUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: ForgotPasswordUiEvent) {
        when (event) {
            ForgotPasswordUiEvent.BackClicked -> sendEffect(ForgotPasswordUiEffect.CloseScreen)
            is ForgotPasswordUiEvent.EmailChanged -> _uiState.update { it.copy(email = event.value, emailErrorResId = null) }
            ForgotPasswordUiEvent.SubmitClicked -> sendOtp()
            ForgotPasswordUiEvent.LoginClicked -> sendEffect(ForgotPasswordUiEffect.NavigateToLogin)
        }
    }

    private fun sendOtp() {
        if (_uiState.value.isLoading) return
        val email = _uiState.value.email.trim()
        if (!EMAIL_PATTERN.matches(email)) {
            _uiState.update {
                it.copy(
                    email = email,
                    emailErrorResId = R.string.register_email_invalid,
                    isLoading = false
                )
            }
            return
        }
        _uiState.update { it.copy(email = email, emailErrorResId = null, isLoading = true) }
        viewModelScope.launch {
            val result = try {
                requestPasswordReset(email)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                AuthResult.REJECTED
            }
            _uiState.update {
                it.copy(isLoading = false, emailErrorResId = R.string.auth_backend_not_configured.takeIf { result != AuthResult.SUCCESS })
            }
            if (result == AuthResult.SUCCESS) sendEffect(ForgotPasswordUiEffect.NavigateToOtp(email))
        }
    }

    private fun sendEffect(effect: ForgotPasswordUiEffect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }

    class Factory(private val requestPasswordReset: RequestPasswordResetUseCase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ForgotPasswordViewModel(requestPasswordReset) as T
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    }
}
