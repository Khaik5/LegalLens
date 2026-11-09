package com.example.lagallens.presentation.feature.profile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.domain.model.EmploymentContractRisk
import com.example.lagallens.presentation.feature.profile.contract.ProfileUiEffect
import com.example.lagallens.presentation.feature.profile.contract.ProfileUiEvent
import com.example.lagallens.presentation.feature.profile.contract.ProfileUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LegalLensApplication
    private val profileRepository = app.userProfileRepository
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<ProfileUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        loadProfile()
        observeActivityCounts()
    }

    private fun observeActivityCounts() {
        viewModelScope.launch {
            try {
                app.contractRepository.observeContracts().collect { contracts ->
                    _uiState.update {
                        it.copy(
                            contractCount = contracts.size,
                            analysisCount = contracts.count { contract -> contract.risk != EmploymentContractRisk.NOT_ANALYZED }
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { it.copy(contractCount = null, analysisCount = null) }
            }
        }
        viewModelScope.launch {
            val conversations = try {
                app.chatbotRepository.getConversations().size
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
            _uiState.update { it.copy(chatCount = conversations) }
        }
    }

    fun onEvent(event: ProfileUiEvent) {
        when (event) {
            is ProfileUiEvent.ActionClicked -> emitEffect(
                ProfileUiEffect.ShowActionUnavailable(event.actionTitleRes)
            )
            ProfileUiEvent.EditProfileClicked -> emitEffect(ProfileUiEffect.NavigateToEditProfile)
            ProfileUiEvent.SettingsClicked -> emitEffect(
                ProfileUiEffect.NavigateToSettings
            )
            ProfileUiEvent.ChangePasswordClicked -> emitEffect(ProfileUiEffect.NavigateToChangePassword)
            ProfileUiEvent.PrivacyClicked -> emitEffect(ProfileUiEffect.NavigateToPrivacy)
            ProfileUiEvent.AboutClicked -> emitEffect(ProfileUiEffect.NavigateToAbout)
            is ProfileUiEvent.ProfileUpdated -> saveProfile(event)
            ProfileUiEvent.LogoutClicked -> emitEffect(ProfileUiEffect.ShowLogoutConfirmation)
            ProfileUiEvent.LogoutConfirmed -> logout()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val profile = profileRepository.getProfile()
                _uiState.update {
                    it.copy(name = profile.name, email = profile.email, phone = profile.phone, photoUri = profile.photoUri, isLoading = false)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, isBackendUnavailable = true) }
            }
        }
    }

    private fun saveProfile(event: ProfileUiEvent.ProfileUpdated) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val profile = profileRepository.saveProfile(event.name, event.phone, event.photoUri)
                _uiState.update {
                    it.copy(name = profile.name, email = profile.email, phone = profile.phone, photoUri = profile.photoUri, isSaving = false, isBackendUnavailable = false)
                }
                _uiEffect.send(ProfileUiEffect.ProfileSaveSucceeded)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false, isBackendUnavailable = true) }
                _uiEffect.send(ProfileUiEffect.ProfileSaveFailed)
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            val signedOut = try {
                app.logoutUseCase()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                false
            }
            _uiEffect.send(if (signedOut) ProfileUiEffect.NavigateToAuthentication else ProfileUiEffect.ShowLogoutFailed)
        }
    }

    private fun emitEffect(effect: ProfileUiEffect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }
}
