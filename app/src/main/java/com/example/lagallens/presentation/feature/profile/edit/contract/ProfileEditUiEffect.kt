package com.example.lagallens.presentation.feature.profile.edit.contract

sealed interface ProfileEditUiEffect {
    data object OpenPhotoPicker : ProfileEditUiEffect
    data class ProfileSaved(val name: String, val phone: String, val photoUri: String?) : ProfileEditUiEffect
}
