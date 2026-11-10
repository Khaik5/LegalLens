package com.example.lagallens.domain.model.auth

enum class OtpPurpose { REGISTRATION, PASSWORD_RESET }

data class OtpChallenge(val challengeId: String, val expiresInSeconds: Int, val resendAfterSeconds: Int)

sealed interface OtpVerification {
    data object Verified : OtpVerification
    data class PasswordResetVerified(val verificationToken: String) : OtpVerification
}

sealed interface AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>
    data class Failed(val reason: AuthFailure) : AuthResult<Nothing>
}

enum class AuthFailure { EMAIL_TAKEN, INVALID_CODE, OTP_RATE_LIMITED, RATE_LIMITED, PHASE_DISABLED, REJECTED }
