package com.example.lagallens.presentation.feature.profile.edit.contract

sealed interface ProfileEditUiEffect {
    data object ShowPhotoPickerUnavailable : ProfileEditUiEffect
    data class ProfileSaved(val name: String, val phone: String) : ProfileEditUiEffect
}


