package com.example.lagallens.presentation.feature.register.contract

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullNameErrorResId: Int? = null,
    val emailErrorResId: Int? = null,
    val passwordErrorResId: Int? = null,
    val confirmPasswordErrorResId: Int? = null,
    val isLoading: Boolean = false
)
