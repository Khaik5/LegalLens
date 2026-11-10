package com.example.lagallens.presentation.feature.otp.contract

sealed interface OtpVerificationUiEvent {
    data class Initialized(
        val email: String,
        val purpose: com.example.lagallens.domain.model.auth.OtpPurpose,
        val challenge: com.example.lagallens.domain.model.auth.OtpChallenge
    ) : OtpVerificationUiEvent
    data object BackClicked : OtpVerificationUiEvent
    data class DigitChanged(val index: Int, val value: String) : OtpVerificationUiEvent
    data object SubmitClicked : OtpVerificationUiEvent
    data object ResendClicked : OtpVerificationUiEvent
}
