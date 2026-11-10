package com.example.lagallens.presentation.feature.resetpassword.contract

data class ResetPasswordUiState(
    val password: String = "",
    val confirmPassword: String = "",
    val passwordErrorResId: Int? = null,
    val confirmPasswordErrorResId: Int? = null,
    val isLoading: Boolean = false
)
