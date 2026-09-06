package com.example.lagallens.presentation.feature.otp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiEffect
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiEvent
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiState

class OtpVerificationViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OtpVerificationUiState())
    val uiState: StateFlow<OtpVerificationUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<OtpVerificationUiEffect>()
    val uiEffect: SharedFlow<OtpVerificationUiEffect> = _uiEffect.asSharedFlow()
    private var countdownJob: Job? = null

    init {
        startCountdown()
    }

    fun onEvent(event: OtpVerificationUiEvent) {
        when (event) {
            is OtpVerificationUiEvent.Initialized -> _uiState.update { it.copy(email = event.email) }
            OtpVerificationUiEvent.BackClicked -> sendEffect(OtpVerificationUiEffect.CloseScreen)
            is OtpVerificationUiEvent.DigitChanged -> updateDigit(event.index, event.value)
            OtpVerificationUiEvent.SubmitClicked -> submitCode()
            OtpVerificationUiEvent.ResendClicked -> resendCode()
        }
    }

    private fun updateDigit(index: Int, value: String) {
        _uiState.update { state ->
            val digits = state.digits.toMutableList()
            digits[index] = value.take(1)
            state.copy(digits = digits, hasError = false)
        }
    }

    private fun submitCode() {
        if (_uiState.value.digits.all { it.isNotEmpty() }) {
            sendEffect(OtpVerificationUiEffect.NavigateToSuccess)
        } else {
            _uiState.update { it.copy(hasError = true) }
        }
    }

    private fun resendCode() {
        if (_uiState.value.isResendAvailable) {
            _uiState.update { it.copy(digits = List(6) { "" }, hasError = false, remainingSeconds = 59) }
            startCountdown()
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(1_000)
                _uiState.update { state ->
                    state.copy(remainingSeconds = (state.remainingSeconds - 1).coerceAtLeast(0))
                }
            }
        }
    }

    private fun sendEffect(effect: OtpVerificationUiEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }
}
