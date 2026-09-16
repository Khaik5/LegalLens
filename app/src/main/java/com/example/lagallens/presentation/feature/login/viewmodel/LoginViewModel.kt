package com.example.lagallens.presentation.feature.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                it.copy(email = event.value, usernameError = null)
            }
            is LoginUiEvent.PasswordChanged -> _uiState.update {
                it.copy(password = event.value, passwordError = null)
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
        val usernameError = if (username.isEmpty()) "Vui lòng nhập tên đăng nhập" else null
        val passwordError = when {
            password.isEmpty() -> "Vui lòng nhập mật khẩu"
            password.length < MINIMUM_PASSWORD_LENGTH -> "Mật khẩu cần ít nhất 6 ký tự"
            else -> null
        }

        if (usernameError != null || passwordError != null) {
            _uiState.update { it.copy(usernameError = usernameError, passwordError = passwordError) }
            return
        }

        if (authenticateUser(username, password)) {
            _uiState.update { it.copy(usernameError = null, passwordError = null) }
            sendEffect(LoginUiEffect.NavigateToMain)
        } else {
            _uiState.update {
                it.copy(usernameError = null, passwordError = "Tên đăng nhập hoặc mật khẩu không đúng")
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
