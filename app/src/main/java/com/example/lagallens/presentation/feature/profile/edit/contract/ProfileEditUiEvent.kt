package com.example.lagallens.presentation.feature.profile.edit.contract

sealed interface ProfileEditUiEvent {
    data class Initialize(val name: String, val email: String, val phone: String) : ProfileEditUiEvent
    data class NameChanged(val value: String) : ProfileEditUiEvent
    data class PhoneChanged(val value: String) : ProfileEditUiEvent
    data object ChangePhotoClicked : ProfileEditUiEvent
    data object SaveClicked : ProfileEditUiEvent
}


