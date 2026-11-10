package com.example.lagallens.presentation.feature.forgotpassword.contract

import com.example.lagallens.domain.model.auth.OtpChallenge

sealed interface ForgotPasswordUiEffect {
    data object CloseScreen : ForgotPasswordUiEffect
    data object NavigateToLogin : ForgotPasswordUiEffect
    data class NavigateToOtp(val email: String, val challenge: OtpChallenge) : ForgotPasswordUiEffect
}
