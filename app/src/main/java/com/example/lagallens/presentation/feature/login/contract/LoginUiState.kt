package com.example.lagallens.presentation.feature.login.contract

data class LoginUiState(
    val email: String = "Admin",
    val password: String = "123456",
    val usernameError: String? = null,
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false
)
