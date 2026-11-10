package com.example.lagallens.data.model.response

data class LoginResponse(
    val accessToken: String?,
    val user: AuthUserResponse?,
    val refreshToken: String?,
    val expiresIn: Long?
)

data class AuthUserResponse(val id: String?, val email: String?, val fullName: String?)
