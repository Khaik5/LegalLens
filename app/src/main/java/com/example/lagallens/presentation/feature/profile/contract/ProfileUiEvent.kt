package com.example.lagallens.presentation.feature.profile.contract

import androidx.annotation.StringRes

sealed interface ProfileUiEvent {
    data class ActionClicked(@StringRes val actionTitleRes: Int) : ProfileUiEvent
    data object EditProfileClicked : ProfileUiEvent
    data object NotificationSettingsClicked : ProfileUiEvent
    data object ChangePasswordClicked : ProfileUiEvent
    data class ProfileUpdated(val name: String, val phone: String) : ProfileUiEvent
    data object LogoutClicked : ProfileUiEvent
    data object LogoutConfirmed : ProfileUiEvent
}
