package com.example.lagallens.presentation.feature.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.usecase.AuthenticateUserUseCase
import com.example.lagallens.presentation.feature.login.contract.LoginUiEffect
import com.example.lagallens.presentation.feature.login.contract.LoginUiEvent
import com.example.lagallens.presentation.feature.login.contract.LoginUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authenticateUser: AuthenticateUserUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<LoginUiEffect>()
    val uiEffect: SharedFlow<LoginUiEffect> = _uiEffect.asSharedFlow()

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
        val usernameErrorResId = if (username.isEmpty()) R.string.login_username_required else null
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

        val authenticated = try {
            authenticateUser(username, password)
        } catch (_: Exception) {
            _uiState.update {
                it.copy(usernameErrorResId = null, passwordErrorResId = R.string.auth_backend_not_configured)
            }
            return
        }
        if (authenticated) {
            _uiState.update { it.copy(usernameErrorResId = null, passwordErrorResId = null) }
            sendEffect(LoginUiEffect.NavigateToMain)
        } else {
            _uiState.update {
                it.copy(usernameErrorResId = null, passwordErrorResId = R.string.login_invalid_credentials)
            }
        }
    }

    private fun sendEffect(effect: LoginUiEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }

    private companion object {
        const val MINIMUM_PASSWORD_LENGTH = 6
    }
}
