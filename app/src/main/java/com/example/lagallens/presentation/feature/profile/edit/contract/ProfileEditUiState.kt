package com.example.lagallens.presentation.feature.profile.edit.contract

import androidx.annotation.StringRes

data class ProfileEditUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUri: String? = null,
    val isInitialized: Boolean = false,
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val phoneErrorRes: Int? = null
)
