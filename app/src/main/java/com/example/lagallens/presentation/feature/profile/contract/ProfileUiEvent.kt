package com.example.lagallens.presentation.feature.profile.contract

import androidx.annotation.StringRes

sealed interface ProfileUiEvent {
    data class ActionClicked(@StringRes val actionTitleRes: Int) : ProfileUiEvent
    data object EditProfileClicked : ProfileUiEvent
    data object SettingsClicked : ProfileUiEvent
    data object PrivacyClicked : ProfileUiEvent
    data object AboutClicked : ProfileUiEvent
    data object ChangePasswordClicked : ProfileUiEvent
    data class ProfileUpdated(val name: String, val phone: String, val photoUri: String? = null) : ProfileUiEvent
    data object LogoutClicked : ProfileUiEvent
    data object LogoutConfirmed : ProfileUiEvent
}
