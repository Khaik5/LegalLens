package com.example.lagallens.presentation.feature.profile.password.contract

import androidx.annotation.StringRes

data class ProfilePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmation: String = "",
    @StringRes val currentPasswordErrorRes: Int? = null,
    @StringRes val newPasswordErrorRes: Int? = null,
    @StringRes val confirmationErrorRes: Int? = null
) {
    val hasMinimumLength: Boolean
        get() = newPassword.length >= MINIMUM_PASSWORD_LENGTH

    val hasMixedCase: Boolean
        get() = newPassword.any { it.isUpperCase() } && newPassword.any { it.isLowerCase() }

    val hasNumberOrSpecial: Boolean
        get() = newPassword.any { it.isDigit() } || newPassword.any { !it.isLetterOrDigit() }

    val meetsSecurityRequirements: Boolean
        get() = hasMinimumLength && hasMixedCase && hasNumberOrSpecial

    private companion object {
        const val MINIMUM_PASSWORD_LENGTH = 8
    }
}


