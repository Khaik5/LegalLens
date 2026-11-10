package com.example.lagallens.data.model.request

data class RegisterRequest(val fullName: String, val email: String, val password: String)
data class ChallengeRequest(val purpose: String, val email: String, val challengePurpose: String? = null)
data class VerifyChallengeRequest(val challengeId: String, val code: String)
data class ResetPasswordRequest(val challengeId: String, val verificationToken: String, val newPassword: String)
