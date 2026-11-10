package com.example.lagallens.presentation.feature.login.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.common.utils.toFailureMessage
import com.example.lagallens.R
import com.example.lagallens.domain.usecase.AuthenticateUserUseCase
import com.example.lagallens.presentation.feature.login.contract.LoginUiEffect
import com.example.lagallens.presentation.feature.login.contract.LoginUiEvent
import com.example.lagallens.presentation.feature.login.contract.LoginUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authenticateUser: AuthenticateUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<LoginUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            LoginUiEvent.BackClicked -> sendEffect(LoginUiEffect.CloseScreen)
            is LoginUiEvent.EmailChanged -> _uiState.update {
                it.copy(email = event.value, usernameErrorResId = null)
            }
            is LoginUiEvent.PasswordChanged -> _uiState.update {
                it.copy(password = event.value, passwordErrorResId = null)
            }
            LoginUiEvent.PasswordVisibilityClicked -> _uiState.update {
                it.copy(isPasswordVisible = !it.isPasswordVisible)
            }
            LoginUiEvent.ForgotPasswordClicked -> {
                sendEffect(LoginUiEffect.NavigateToForgotPassword)
            }
            LoginUiEvent.RegisterClicked -> sendEffect(LoginUiEffect.NavigateToRegister)
            LoginUiEvent.SubmitClicked -> authenticate()
        }
    }

    private fun authenticate() {
        val state = _uiState.value
        val username = state.email.trim()
        val password = state.password
        val usernameErrorResId = when {
            username.isEmpty() -> R.string.login_username_required
            !Patterns.EMAIL_ADDRESS.matcher(username).matches() -> R.string.register_email_invalid
            else -> null
        }
        val passwordErrorResId = when {
            password.isEmpty() -> R.string.login_password_required
            password.length < MINIMUM_PASSWORD_LENGTH -> R.string.login_password_minimum_length
            else -> null
        }

        if (usernameErrorResId != null || passwordErrorResId != null) {
            _uiState.update {
                it.copy(usernameErrorResId = usernameErrorResId, passwordErrorResId = passwordErrorResId)
            }
            return
        }

        if (state.isLoading) return
        _uiState.update { it.copy(usernameErrorResId = null, passwordErrorResId = null, isLoading = true) }
        viewModelScope.launch {
            val authenticated = try {
                authenticateUser(username, password)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                _uiState.update { it.copy(passwordErrorResId = failure.toFailureMessage(), isLoading = false) }
                return@launch
            }
            if (authenticated) {
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(LoginUiEffect.NavigateToMain)
            } else {
                _uiState.update { it.copy(passwordErrorResId = R.string.login_invalid_credentials, isLoading = false) }
            }
        }
    }

    private fun sendEffect(effect: LoginUiEffect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }

    private companion object {
        const val MINIMUM_PASSWORD_LENGTH = 8
    }
}
