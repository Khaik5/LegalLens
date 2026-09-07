package com.example.lagallens.presentation.feature.forgotpassword.viewmodel

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
import com.example.lagallens.presentation.feature.forgotpassword.contract.ForgotPasswordUiEffect
import com.example.lagallens.presentation.feature.forgotpassword.contract.ForgotPasswordUiEvent
import com.example.lagallens.presentation.feature.forgotpassword.contract.ForgotPasswordUiState

class ForgotPasswordViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ForgotPasswordUiEffect>()
    val uiEffect: SharedFlow<ForgotPasswordUiEffect> = _uiEffect.asSharedFlow()

    fun onEvent(event: ForgotPasswordUiEvent) {
        when (event) {
            ForgotPasswordUiEvent.BackClicked -> sendEffect(ForgotPasswordUiEffect.CloseScreen)
            is ForgotPasswordUiEvent.EmailChanged -> _uiState.update { it.copy(email = event.value, emailError = null) }
            ForgotPasswordUiEvent.SubmitClicked -> sendOtp()
            ForgotPasswordUiEvent.LoginClicked -> sendEffect(ForgotPasswordUiEffect.NavigateToLogin)
        }
    }

    private fun sendOtp() {
        val email = _uiState.value.email.trim()
        if (!EMAIL_PATTERN.matches(email)) {
            _uiState.update {
                it.copy(
                    email = email,
                    emailError = "Vui lòng nhập email hợp lệ",
                    isLoading = false
                )
            }
            return
        }
        _uiState.update { it.copy(email = email, emailError = null, isLoading = true) }
        _uiState.update { it.copy(isLoading = false) }
        sendEffect(ForgotPasswordUiEffect.NavigateToOtp(email))
    }

    private fun sendEffect(effect: ForgotPasswordUiEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    }
}
