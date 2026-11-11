package com.example.lagallens.presentation.feature.login.contract

import com.example.lagallens.domain.model.auth.OtpChallenge

sealed interface LoginUiEffect {
    /** The account is not verified yet; a new code was sent to [email]. */
    data class NavigateToAccountVerification(val email: String, val challenge: OtpChallenge) : LoginUiEffect
    data object CloseScreen : LoginUiEffect
    data object NavigateToForgotPassword : LoginUiEffect
    data object NavigateToRegister : LoginUiEffect
    data object NavigateToMain : LoginUiEffect
}
