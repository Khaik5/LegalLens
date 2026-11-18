package com.example.lagallens.presentation.feature.profile.email.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.core.utils.isValidEmail
import com.example.lagallens.core.network.ApiFailureKind
import com.example.lagallens.core.network.ApiFailureMapper
import com.example.lagallens.domain.repository.EmailAlreadyUsedException
import com.example.lagallens.domain.repository.EmailChangeCodeRejectedException
import com.example.lagallens.domain.repository.EmailChangeRejectedException
import com.example.lagallens.domain.repository.ProfileRepository
import com.example.lagallens.presentation.feature.profile.email.contract.ProfileEmailChangeStep
import com.example.lagallens.presentation.feature.profile.email.contract.ProfileEmailChangeUiEffect
import com.example.lagallens.presentation.feature.profile.email.contract.ProfileEmailChangeUiEvent
import com.example.lagallens.presentation.feature.profile.email.contract.ProfileEmailChangeUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileEmailChangeViewModel(
    private val profiles: ProfileRepository,
    private val currentEmail: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileEmailChangeUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<ProfileEmailChangeUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var challengeId: String? = null

    fun onEvent(event: ProfileEmailChangeUiEvent) {
        when (event) {
            is ProfileEmailChangeUiEvent.NewEmailChanged ->
                _uiState.update { it.copy(newEmail = event.value, emailErrorRes = null) }
            is ProfileEmailChangeUiEvent.CurrentPasswordChanged ->
                _uiState.update { it.copy(currentPassword = event.value, passwordErrorRes = null) }
            is ProfileEmailChangeUiEvent.CodeChanged ->
                _uiState.update { it.copy(code = event.value, codeErrorRes = null) }
            ProfileEmailChangeUiEvent.SendCodeClicked -> sendCode()
            ProfileEmailChangeUiEvent.ConfirmClicked -> confirm()
        }
    }

    private fun sendCode() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val email = state.newEmail.trim()
        val emailError = when {
            !isValidEmail(email) -> R.string.profile_email_change_email_invalid
            email.equals(currentEmail.trim(), ignoreCase = true) -> R.string.profile_email_change_email_same
            else -> null
        }
        val passwordError = if (state.currentPassword.isEmpty()) R.string.profile_password_current_required else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailErrorRes = emailError, passwordErrorRes = passwordError) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                challengeId = profiles.requestEmailChange(email, state.currentPassword).challengeId
                _uiState.update {
                    it.copy(step = ProfileEmailChangeStep.CONFIRM, newEmail = email, currentPassword = "", isSubmitting = false)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                when (failure) {
                    is EmailChangeRejectedException -> _uiState.update {
                        it.copy(isSubmitting = false, passwordErrorRes = R.string.profile_email_change_password_incorrect)
                    }
                    is EmailAlreadyUsedException -> _uiState.update {
                        it.copy(isSubmitting = false, emailErrorRes = R.string.profile_email_change_email_used)
                    }
                    else -> {
                        _uiState.update { it.copy(isSubmitting = false) }
                        _uiEffect.send(ProfileEmailChangeUiEffect.ShowError(messageFor(failure)))
                    }
                }
            }
        }
    }

    private fun confirm() {
        val state = _uiState.value
        val id = challengeId
        if (state.isSubmitting || id == null) return
        val code = state.code.trim()
        if (code.length != CODE_LENGTH || !code.all(Char::isDigit)) {
            _uiState.update { it.copy(codeErrorRes = R.string.profile_email_change_code_invalid) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                profiles.confirmEmailChange(id, code)
                _uiEffect.send(ProfileEmailChangeUiEffect.NavigateToLogin)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                when (failure) {
                    is EmailChangeCodeRejectedException -> _uiState.update {
                        it.copy(isSubmitting = false, codeErrorRes = R.string.profile_email_change_code_invalid)
                    }
                    is EmailAlreadyUsedException -> {
                        // Someone else took the address meanwhile; the user picks another one.
                        challengeId = null
                        _uiState.update {
                            it.copy(
                                step = ProfileEmailChangeStep.REQUEST,
                                code = "",
                                isSubmitting = false,
                                emailErrorRes = R.string.profile_email_change_email_used
                            )
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(isSubmitting = false) }
                        _uiEffect.send(ProfileEmailChangeUiEffect.ShowError(messageFor(failure)))
                    }
                }
            }
        }
    }

    private fun messageFor(failure: Exception): Int {
        val api = ApiFailureMapper.map(failure)
        return when {
            api.kind == ApiFailureKind.NETWORK -> R.string.profile_edit_no_connection
            api.statusCode == 429 -> R.string.profile_password_rate_limited
            else -> R.string.profile_email_change_unavailable
        }
    }

    class Factory(private val profiles: ProfileRepository, private val currentEmail: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProfileEmailChangeViewModel(profiles, currentEmail) as T
    }

    private companion object {
        const val CODE_LENGTH = 6
    }
}
