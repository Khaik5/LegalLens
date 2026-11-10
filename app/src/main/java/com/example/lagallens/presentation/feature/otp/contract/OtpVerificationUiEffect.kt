package com.example.lagallens.presentation.feature.otp.contract

sealed interface OtpVerificationUiEffect {
    data object CloseScreen : OtpVerificationUiEffect
    data object NavigateToSuccess : OtpVerificationUiEffect
    data class NavigateToResetPassword(val challengeId: String, val verificationToken: String) : OtpVerificationUiEffect
}
