package com.example.lagallens.presentation.feature.otp.viewmodel

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.lagallens.R
import com.example.lagallens.domain.model.auth.AuthFailure
import com.example.lagallens.domain.model.auth.AuthResult
import com.example.lagallens.domain.model.auth.OtpChallenge
import com.example.lagallens.domain.model.auth.OtpPurpose
import com.example.lagallens.domain.model.auth.OtpVerification
import com.example.lagallens.domain.usecase.ResendOtpUseCase
import com.example.lagallens.domain.usecase.VerifyOtpUseCase
import com.example.lagallens.presentation.common.utils.toAuthMessage
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiEffect
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiEvent
import com.example.lagallens.presentation.feature.otp.contract.OtpVerificationUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OtpVerificationViewModel(
    private val verifyOtp: VerifyOtpUseCase,
    private val resendOtp: ResendOtpUseCase,
    private val savedState: SavedStateHandle,
    private val nowMillis: () -> Long = SystemClock::elapsedRealtime
) : ViewModel() {
    private val _uiState = MutableStateFlow(OtpVerificationUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<OtpVerificationUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()
    private var countdownJob: Job? = null

    fun onEvent(event: OtpVerificationUiEvent) {
        when (event) {
            is OtpVerificationUiEvent.Initialized -> initialize(event)
            OtpVerificationUiEvent.BackClicked -> _uiEffect.trySend(OtpVerificationUiEffect.CloseScreen)
            is OtpVerificationUiEvent.DigitChanged -> _uiState.update {
                val digits = it.digits.toMutableList()
                digits[event.index] = event.value.take(1)
                it.copy(digits = digits, errorResId = null)
            }
            OtpVerificationUiEvent.SubmitClicked -> submitCode()
            OtpVerificationUiEvent.ResendClicked -> resendCode()
        }
    }

    private fun initialize(event: OtpVerificationUiEvent.Initialized) {
        if (_uiState.value.email.isNotBlank()) return
        val purpose = savedState.get<String>(PURPOSE)?.let(OtpPurpose::valueOf) ?: event.purpose
        savedState[PURPOSE] = purpose.name
        _uiState.update { it.copy(email = event.email, purpose = purpose) }
        if (savedState.get<String>(CHALLENGE_ID) == null) saveChallenge(event.challenge)
        startCountdown()
    }

    private fun saveChallenge(challenge: OtpChallenge) {
        val now = nowMillis()
        savedState[CHALLENGE_ID] = challenge.challengeId
        savedState[EXPIRES_AT] = now + challenge.expiresInSeconds * SECOND
        savedState[RESEND_AT] = now + challenge.resendAfterSeconds * SECOND
    }

    private fun submitCode() {
        val state = _uiState.value
        if (state.isVerifying || state.isResending || state.isVerified) return
        val challengeId = savedState.get<String>(CHALLENGE_ID)
        val expiresAt = savedState.get<Long>(EXPIRES_AT) ?: 0L
        val code = state.digits.joinToString("")
        if (challengeId.isNullOrBlank() || nowMillis() >= expiresAt || code.length != 6 || !code.all { it in '0'..'9' }) {
            _uiState.update { it.copy(errorResId = R.string.otp_invalid) }
            return
        }
        _uiState.update { it.copy(isVerifying = true, errorResId = null) }
        viewModelScope.launch {
            val result = try {
                verifyOtp(challengeId, code, state.purpose)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                AuthResult.Failed(AuthFailure.REJECTED)
            }
            when (result) {
                is AuthResult.Failed -> _uiState.update {
                    it.copy(isVerifying = false, errorResId = result.reason.toAuthMessage())
                }
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isVerifying = false, isVerified = true) }
                    val effect = when (val verified = result.value) {
                        OtpVerification.Verified -> OtpVerificationUiEffect.NavigateToSuccess
                        is OtpVerification.PasswordResetVerified ->
                            OtpVerificationUiEffect.NavigateToResetPassword(challengeId, verified.verificationToken)
                    }
                    _uiEffect.send(effect)
                }
            }
        }
    }

    private fun resendCode() {
        val state = _uiState.value
        if (!state.isResendAvailable) return
        _uiState.update { it.copy(isResending = true, errorResId = null) }
        viewModelScope.launch {
            val result = try {
                resendOtp(state.email, state.purpose)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                AuthResult.Failed(AuthFailure.REJECTED)
            }
            when (result) {
                is AuthResult.Failed -> _uiState.update {
                    it.copy(isResending = false, errorResId = result.reason.toAuthMessage())
                }
                is AuthResult.Success -> {
                    saveChallenge(result.value)
                    _uiState.update { it.copy(isResending = false, isVerified = false, digits = List(6) { "" }) }
                    startCountdown()
                }
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        val resendAt = savedState.get<Long>(RESEND_AT) ?: 0L
        updateRemainingSeconds(resendAt)
        countdownJob = viewModelScope.launch {
            while (_uiState.value.remainingSeconds > 0) {
                delay(SECOND)
                updateRemainingSeconds(resendAt)
            }
        }
    }

    private fun updateRemainingSeconds(resendAt: Long) {
        val remaining = (resendAt - nowMillis()).coerceAtLeast(0)
        _uiState.update { it.copy(remainingSeconds = ((remaining + SECOND - 1) / SECOND).toInt()) }
    }

    class Factory(
        private val verifyOtp: VerifyOtpUseCase,
        private val resendOtp: ResendOtpUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            OtpVerificationViewModel(verifyOtp, resendOtp, extras.createSavedStateHandle()) as T
    }

    private companion object {
        const val SECOND = 1000L
        const val CHALLENGE_ID = "challengeId"
        const val PURPOSE = "purpose"
        const val EXPIRES_AT = "expiresAt"
        const val RESEND_AT = "resendAt"
    }
}
