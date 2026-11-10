package com.example.lagallens.data.mapper

import com.example.lagallens.core.network.ApiFailure
import com.example.lagallens.core.network.ApiFailureKind
import com.example.lagallens.core.network.EmptyApiResponseException
import com.example.lagallens.data.model.response.ChallengeResponse
import com.example.lagallens.data.model.response.RegisterResponse
import com.example.lagallens.domain.model.auth.AuthFailure
import com.example.lagallens.domain.model.auth.OtpChallenge
import com.example.lagallens.domain.model.auth.OtpPurpose

internal fun RegisterResponse.toOtpChallenge(): OtpChallenge? {
    if (challengeId == null && accountStatus != "PENDING_VERIFICATION") return null
    return ChallengeResponse(challengeId, expiresIn, resendAfter).toOtpChallenge()
}

internal fun ChallengeResponse.toOtpChallenge(): OtpChallenge {
    val id = challengeId
    val expiry = expiresIn
    val cooldown = resendAfter
    if (id.isNullOrBlank() || expiry == null || expiry <= 0 || cooldown == null || cooldown < 0) {
        throw EmptyApiResponseException("OTP challenge")
    }
    return OtpChallenge(id, expiry, cooldown)
}

internal fun OtpPurpose.toApiPurpose() = when (this) {
    OtpPurpose.REGISTRATION -> "REGISTER"
    OtpPurpose.PASSWORD_RESET -> "RESET_PASSWORD"
}

internal fun ApiFailure.toAuthFailure(): AuthFailure = when (details?.code) {
    "EMAIL_EXISTS", "CONFLICT" -> AuthFailure.EMAIL_TAKEN
    "OTP_INVALID" -> AuthFailure.INVALID_CODE
    "OTP_RATE_LIMITED" -> AuthFailure.OTP_RATE_LIMITED
    "AUTH_PHASE02_DISABLED" -> AuthFailure.PHASE_DISABLED
    else -> when (kind) {
        ApiFailureKind.CONFLICT -> AuthFailure.EMAIL_TAKEN
        ApiFailureKind.RATE_LIMITED -> AuthFailure.RATE_LIMITED
        else -> AuthFailure.REJECTED
    }
}
