package com.example.lagallens.presentation.feature.forgotpassword.contract

data class ForgotPasswordUiState(
    val email: String = "",
    val emailErrorResId: Int? = null,
    val isLoading: Boolean = false
)
