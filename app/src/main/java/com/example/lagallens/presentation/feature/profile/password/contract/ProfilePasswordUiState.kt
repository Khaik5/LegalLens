package com.example.lagallens.presentation.feature.profile.password.contract

import androidx.annotation.StringRes

data class ProfilePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmation: String = "",
    val isSubmitting: Boolean = false,
    @StringRes val currentPasswordErrorRes: Int? = null,
    @StringRes val newPasswordErrorRes: Int? = null,
    @StringRes val confirmationErrorRes: Int? = null
) {
    val hasMinimumLength: Boolean
        get() = newPassword.length >= MINIMUM_PASSWORD_LENGTH

    val hasMaximumLength: Boolean
        get() = newPassword.length <= MAXIMUM_PASSWORD_LENGTH

    val meetsSecurityRequirements: Boolean
        get() = hasMinimumLength && hasMaximumLength

    private companion object {
        const val MINIMUM_PASSWORD_LENGTH = 8
        const val MAXIMUM_PASSWORD_LENGTH = 72
    }
}
