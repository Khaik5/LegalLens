package com.example.lagallens.presentation.feature.profile.password.contract


sealed interface ProfilePasswordUiEffect {
    data object NavigateBack : ProfilePasswordUiEffect

    data object ShowBackendUnavailable : ProfilePasswordUiEffect
    data object PasswordUpdated : ProfilePasswordUiEffect
    data object CurrentPasswordIncorrect : ProfilePasswordUiEffect
}
