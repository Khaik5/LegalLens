package com.example.lagallens.presentation.common.utils

import com.example.lagallens.R
import com.example.lagallens.domain.model.auth.AuthFailure

internal fun AuthFailure.toAuthMessage() = when (this) {
    AuthFailure.EMAIL_TAKEN -> R.string.register_email_taken
    AuthFailure.INVALID_CODE -> R.string.otp_invalid
    AuthFailure.OTP_RATE_LIMITED -> R.string.otp_rate_limited
    AuthFailure.RATE_LIMITED -> R.string.auth_rate_limited
    AuthFailure.PHASE_DISABLED -> R.string.auth_phase_disabled
    AuthFailure.REJECTED -> R.string.auth_request_failed
}
