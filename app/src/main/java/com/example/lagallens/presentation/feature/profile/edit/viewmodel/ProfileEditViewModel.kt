package com.example.lagallens.presentation.feature.profile.edit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.profile.edit.contract.ProfileEditUiEffect
import com.example.lagallens.presentation.feature.profile.edit.contract.ProfileEditUiEvent
import com.example.lagallens.presentation.feature.profile.edit.contract.ProfileEditUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileEditViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProfileEditUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

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
                ProfileEditUiEffect.ShowPhotoPickerUnavailable
            )
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
                    isInitialized = true
                )
            }
        }
    }

    private fun saveProfile() {
        val state = _uiState.value
        val name = state.name.trim()
        val phone = state.phone.trim()
        val nameErrorRes = if (name.isBlank()) R.string.profile_edit_name_required else null
        val phoneErrorRes = when {
            phone.isBlank() -> R.string.profile_edit_phone_required
            phone.filter(Char::isDigit).length < MIN_PHONE_DIGITS -> R.string.profile_edit_phone_invalid
            else -> null
        }

        if (nameErrorRes != null || phoneErrorRes != null) {
            _uiState.update {
                it.copy(nameErrorRes = nameErrorRes, phoneErrorRes = phoneErrorRes)
            }
            return
        }

        emitEffect(ProfileEditUiEffect.ProfileSaved(name, phone))
    }

    private fun emitEffect(effect: ProfileEditUiEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }

    private companion object {
        const val MIN_PHONE_DIGITS = 9
    }
}


