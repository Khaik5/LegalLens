package com.example.lagallens.presentation.feature.profile.contract

import androidx.annotation.StringRes

sealed interface ProfileUiEffect {
    data class ShowActionUnavailable(@StringRes val actionTitleRes: Int) : ProfileUiEffect
    data object NavigateToEditProfile : ProfileUiEffect
    data object NavigateToNotificationSettings : ProfileUiEffect
    data object NavigateToChangePassword : ProfileUiEffect
    data object ShowLogoutConfirmation : ProfileUiEffect
    data object NavigateToAuthentication : ProfileUiEffect
}
