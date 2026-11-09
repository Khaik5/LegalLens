package com.example.lagallens.presentation.feature.otp.contract

sealed interface OtpVerificationUiEffect {
    data object CloseScreen : OtpVerificationUiEffect
    data object NavigateToSuccess : OtpVerificationUiEffect
    data class NavigateToResetPassword(val email: String) : OtpVerificationUiEffect
    data class ShowMessage(@androidx.annotation.StringRes val messageRes: Int) : OtpVerificationUiEffect
}
