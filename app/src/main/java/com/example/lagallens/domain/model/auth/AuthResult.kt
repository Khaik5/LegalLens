package com.example.lagallens.domain.model.auth

enum class OtpPurpose { REGISTRATION, PASSWORD_RESET }

enum class AuthResult { SUCCESS, EMAIL_TAKEN, INVALID_CODE, REJECTED }
