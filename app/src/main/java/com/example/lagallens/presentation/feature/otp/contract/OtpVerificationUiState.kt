package com.example.lagallens.presentation.feature.otp.contract

data class OtpVerificationUiState(
    val email: String = "",
    val purpose: com.example.lagallens.domain.model.auth.OtpPurpose = com.example.lagallens.domain.model.auth.OtpPurpose.REGISTRATION,
    val digits: List<String> = List(6) { "" },
    val isVerifying: Boolean = false,
    val hasError: Boolean = false,
    val remainingSeconds: Int = 59
) {
    val isResendAvailable: Boolean
        get() = remainingSeconds == 0
}
