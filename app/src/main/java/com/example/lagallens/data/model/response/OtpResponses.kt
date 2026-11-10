package com.example.lagallens.data.model.response

data class RegisterResponse(
    val id: String?,
    val email: String?,
    val fullName: String?,
    val accountStatus: String?,
    val challengeId: String?,
    val expiresIn: Int?,
    val resendAfter: Int?
)

data class ChallengeResponse(val challengeId: String?, val expiresIn: Int?, val resendAfter: Int?)
data class VerifyChallengeResponse(val verified: Boolean, val verificationToken: String?)
