package com.example.lagallens.presentation.feature.otp.contract

import com.example.lagallens.domain.model.auth.OtpPurpose

data class OtpVerificationUiState(
    val email: String = "",
    val purpose: OtpPurpose = OtpPurpose.REGISTRATION,
    val digits: List<String> = List(6) { "" },
    val isVerifying: Boolean = false,
    val isResending: Boolean = false,
    val isVerified: Boolean = false,
    val errorResId: Int? = null,
    val remainingSeconds: Int = 0
) {
    val isResendAvailable: Boolean
        get() = email.isNotBlank() && remainingSeconds == 0 && !isVerifying && !isResending
}
