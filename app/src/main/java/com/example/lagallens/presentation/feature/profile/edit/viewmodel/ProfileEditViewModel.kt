package com.example.lagallens.presentation.feature.profile.edit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.profile.edit.contract.ProfileEditUiEffect
import com.example.lagallens.presentation.feature.profile.edit.contract.ProfileEditUiEvent
import com.example.lagallens.presentation.feature.profile.edit.contract.ProfileEditUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileEditViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<ProfileEditUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onEvent(event: ProfileEditUiEvent) {
        when (event) {
            is ProfileEditUiEvent.Initialize -> initialize(event)
            is ProfileEditUiEvent.NameChanged -> _uiState.update {
                it.copy(name = event.value, nameErrorRes = null)
            }
            is ProfileEditUiEvent.PhoneChanged -> _uiState.update {
                it.copy(phone = event.value, phoneErrorRes = null)
            }
            ProfileEditUiEvent.ChangePhotoClicked -> emitEffect(
                ProfileEditUiEffect.OpenPhotoPicker
            )
            is ProfileEditUiEvent.PhotoSelected -> _uiState.update { it.copy(photoUri = event.uri) }
            ProfileEditUiEvent.SaveClicked -> saveProfile()
        }
    }

    private fun initialize(event: ProfileEditUiEvent.Initialize) {
        _uiState.update { state ->
            if (state.isInitialized) state else {
                state.copy(
                    name = event.name,
                    email = event.email,
                    phone = event.phone,
                    photoUri = event.photoUri,
                    isInitialized = true
                )
            }
        }
    }

    private fun saveProfile() {
        val state = _uiState.value
        val name = state.name.trim()
        val phone = state.phone.trim()
        val nameErrorRes = when {
            name.isBlank() -> R.string.profile_edit_name_required
            name.codePointCount(0, name.length) > MAX_NAME_LENGTH -> R.string.profile_edit_name_too_long
            else -> null
        }
        // The phone number is optional; the backend makes the final check that it is a Vietnamese number.
        val phoneErrorRes = when {
            phone.isBlank() -> null
            phone.filter(Char::isDigit).length < MIN_PHONE_DIGITS -> R.string.profile_edit_phone_invalid
            else -> null
        }

        if (nameErrorRes != null || phoneErrorRes != null) {
            _uiState.update {
                it.copy(nameErrorRes = nameErrorRes, phoneErrorRes = phoneErrorRes)
            }
            return
        }

        emitEffect(ProfileEditUiEffect.ProfileSaved(name, phone, state.photoUri))
    }

    private fun emitEffect(effect: ProfileEditUiEffect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }

    private companion object {
        const val MIN_PHONE_DIGITS = 9
        const val MAX_NAME_LENGTH = 100
    }
}
