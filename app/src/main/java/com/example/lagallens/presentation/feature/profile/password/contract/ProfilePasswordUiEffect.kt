package com.example.lagallens.presentation.feature.profile.password.contract

import androidx.annotation.StringRes

sealed interface ProfilePasswordUiEffect {
    data object NavigateBack : ProfilePasswordUiEffect
    data object NavigateToLogin : ProfilePasswordUiEffect
    data object PasswordUpdated : ProfilePasswordUiEffect
    data class ShowError(@param:StringRes val messageRes: Int) : ProfilePasswordUiEffect
}
