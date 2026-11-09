package com.example.lagallens.presentation.feature.otp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.model.auth.OtpPurpose
import com.example.lagallens.domain.usecase.ResendOtpUseCase
import com.example.lagallens.domain.usecase.VerifyOtpUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiEffect
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiEvent
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiState

class OtpVerificationViewModel(
    private val verifyOtp: VerifyOtpUseCase,
    private val resendOtp: ResendOtpUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(OtpVerificationUiState())
    val uiState: StateFlow<OtpVerificationUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<OtpVerificationUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()
    private var countdownJob: Job? = null

    init {
        startCountdown()
    }

    fun onEvent(event: OtpVerificationUiEvent) {
        when (event) {
            is OtpVerificationUiEvent.Initialized -> _uiState.update { it.copy(email = event.email, purpose = event.purpose) }
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
        val state = _uiState.value
        if (state.isVerifying) return
        if (state.digits.any { it.isEmpty() }) {
            _uiState.update { it.copy(hasError = true) }
            return
        }
        _uiState.update { it.copy(isVerifying = true) }
        viewModelScope.launch {
            val result = try {
                verifyOtp(state.email, state.digits.joinToString(""), state.purpose)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                AuthResult.REJECTED
            }
            _uiState.update { it.copy(isVerifying = false, hasError = result == AuthResult.INVALID_CODE) }
            when (result) {
                AuthResult.SUCCESS -> sendEffect(
                    if (state.purpose == OtpPurpose.REGISTRATION) OtpVerificationUiEffect.NavigateToSuccess
                    else OtpVerificationUiEffect.NavigateToResetPassword(state.email)
                )
                AuthResult.INVALID_CODE -> Unit
                AuthResult.EMAIL_TAKEN -> sendEffect(OtpVerificationUiEffect.ShowMessage(R.string.register_email_taken))
                AuthResult.REJECTED -> sendEffect(OtpVerificationUiEffect.ShowMessage(R.string.auth_backend_not_configured))
            }
        }
    }

    private fun resendCode() {
        val state = _uiState.value
        if (!state.isResendAvailable) return
        _uiState.update { it.copy(digits = List(6) { "" }, hasError = false, remainingSeconds = 59) }
        startCountdown()
        viewModelScope.launch {
            val delivered = try {
                resendOtp(state.email, state.purpose) == AuthResult.SUCCESS
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                false
            }
            if (!delivered) sendEffect(OtpVerificationUiEffect.ShowMessage(R.string.auth_backend_not_configured))
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
            _uiEffect.send(effect)
        }
    }

    class Factory(
        private val verifyOtp: VerifyOtpUseCase,
        private val resendOtp: ResendOtpUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = OtpVerificationViewModel(verifyOtp, resendOtp) as T
    }
}
