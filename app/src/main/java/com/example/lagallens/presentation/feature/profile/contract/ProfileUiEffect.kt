package com.example.lagallens.presentation.feature.profile.contract

import androidx.annotation.StringRes

sealed interface ProfileUiEffect {
    data class ShowActionUnavailable(@StringRes val actionTitleRes: Int) : ProfileUiEffect
    data object NavigateToEditProfile : ProfileUiEffect
    data object NavigateToSettings : ProfileUiEffect
    data object NavigateToLanguage : ProfileUiEffect
    data object NavigateToPrivacy : ProfileUiEffect
    data object NavigateToMyDrafts : ProfileUiEffect
    data object NavigateToAbout : ProfileUiEffect
    data object NavigateToChangePassword : ProfileUiEffect
    data object ShowLogoutConfirmation : ProfileUiEffect
    data object NavigateToAuthentication : ProfileUiEffect
    data object ShowLogoutFailed : ProfileUiEffect
    data object ProfileSaveSucceeded : ProfileUiEffect
    data object AvatarRemoved : ProfileUiEffect
    data class ProfileSaveFailed(@param:StringRes val messageRes: Int) : ProfileUiEffect
}
