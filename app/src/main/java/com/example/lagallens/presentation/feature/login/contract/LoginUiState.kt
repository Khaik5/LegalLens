package com.example.lagallens.presentation.feature.login.contract

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val usernameErrorResId: Int? = null,
    val passwordErrorResId: Int? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false
)
