package com.example.lagallens.presentation.feature.register.contract

import com.example.lagallens.domain.model.auth.OtpChallenge

sealed interface RegisterUiEffect {
    data object CloseScreen : RegisterUiEffect
    data object NavigateToLogin : RegisterUiEffect
    data class NavigateToOtp(val email: String, val challenge: OtpChallenge) : RegisterUiEffect
}
