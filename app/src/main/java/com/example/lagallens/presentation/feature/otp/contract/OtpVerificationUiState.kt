package com.example.lagallens.presentation.feature.otp.contract

data class OtpVerificationUiState(
    val email: String = "",
    val digits: List<String> = List(6) { "" },
    val hasError: Boolean = false,
    val remainingSeconds: Int = 59
) {
    val isResendAvailable: Boolean
        get() = remainingSeconds == 0
}
