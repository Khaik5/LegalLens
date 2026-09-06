package com.example.lagallens.presentation.feature.forgotpassword.contract

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false
)
