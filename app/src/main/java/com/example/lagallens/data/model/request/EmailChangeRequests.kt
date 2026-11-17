package com.example.lagallens.data.model.request

data class RequestEmailChangeRequest(val email: String, val currentPassword: String)

data class ConfirmEmailChangeRequest(val challengeId: String, val code: String)
