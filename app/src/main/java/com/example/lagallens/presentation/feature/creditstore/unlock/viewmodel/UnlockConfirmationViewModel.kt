package com.example.lagallens.presentation.feature.creditstore.unlock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.repository.CreditRepository
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEffect
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEvent
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UnlockConfirmationViewModel(private val creditRepository: CreditRepository) : ViewModel() {
    private val ownerSession = creditRepository.activeSessionKey()
    private val _uiState = MutableStateFlow(UnlockConfirmationUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<UnlockConfirmationUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: UnlockConfirmationUiEvent) {
        when (event) {
            is UnlockConfirmationUiEvent.ScreenOpened -> _uiState.update {
                it.copy(analysisResultId = event.analysisResultId,
                    availableCredits = if (creditRepository.isConfigured) creditRepository.balance.value else 0,
                    unlockCost = event.unlockCost.coerceAtLeast(0), isConfirmationRequested = false,
                    isInsufficientCreditsRequested = false,
                    isBackendUnavailable = !creditRepository.isConfigured)
            }

            UnlockConfirmationUiEvent.BackClicked,
            UnlockConfirmationUiEvent.CancelClicked -> emitEffect(UnlockConfirmationUiEffect.NavigateBack)

            UnlockConfirmationUiEvent.ConfirmClicked -> requestUnlock()
        }
    }

    private fun requestUnlock() {
        if (creditRepository.activeSessionKey() != ownerSession) return
        val state = _uiState.value
        if (state.isConfirmationRequested || state.isInsufficientCreditsRequested) {
            return
        }
        if (!creditRepository.isConfigured) {
            emitEffect(UnlockConfirmationUiEffect.ShowBackendUnavailable)
            return
        }
        if (state.availableCredits < state.unlockCost) {
            _uiState.update { it.copy(isInsufficientCreditsRequested = true) }
            emitEffect(UnlockConfirmationUiEffect.NavigateToInsufficientCredits)
            return
        }
        val repository = creditRepository
        val session = repository.activeSessionKey()
        _uiState.update { it.copy(isConfirmationRequested = true) }
        viewModelScope.launch {
            if (repository.activeSessionKey() != session) return@launch
            val receipt = runCatching { repository.spend("unlock:${state.analysisResultId}", state.unlockCost) }.getOrNull()
            if (repository.activeSessionKey() != session) return@launch
            if (receipt == null || (!receipt.applied && receipt.balance < state.unlockCost)) {
                _uiState.update { it.copy(availableCredits = receipt?.balance ?: repository.balance.value, isConfirmationRequested = false, isInsufficientCreditsRequested = true) }
                emitEffect(UnlockConfirmationUiEffect.NavigateToInsufficientCredits)
                return@launch
            }
            _uiState.update { it.copy(availableCredits = receipt.balance) }
            viewModelScope.launch {
                if (repository.activeSessionKey() == session) _uiEffect.emit(UnlockConfirmationUiEffect.UnlockRequested(state.analysisResultId, state.unlockCost))
            }
        }
    }

    private fun emitEffect(effect: UnlockConfirmationUiEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }

    class Factory(private val repository: CreditRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = UnlockConfirmationViewModel(repository) as T
    }
}
