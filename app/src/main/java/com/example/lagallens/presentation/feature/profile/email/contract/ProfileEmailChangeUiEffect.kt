package com.example.lagallens.presentation.feature.profile.email.contract

import androidx.annotation.StringRes

sealed interface ProfileEmailChangeUiEffect {
    /** The email changed and the backend ended every session, so the user signs in again. */
    data object NavigateToLogin : ProfileEmailChangeUiEffect
    data class ShowError(@param:StringRes val messageRes: Int) : ProfileEmailChangeUiEffect
}
