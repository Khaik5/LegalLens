package com.example.lagallens.presentation.feature.profile.email.contract

import androidx.annotation.StringRes

enum class ProfileEmailChangeStep { REQUEST, CONFIRM }

data class ProfileEmailChangeUiState(
    val step: ProfileEmailChangeStep = ProfileEmailChangeStep.REQUEST,
    val newEmail: String = "",
    val currentPassword: String = "",
    val code: String = "",
    val isSubmitting: Boolean = false,
    @StringRes val emailErrorRes: Int? = null,
    @StringRes val passwordErrorRes: Int? = null,
    @StringRes val codeErrorRes: Int? = null
)
