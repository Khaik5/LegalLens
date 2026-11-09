package com.example.lagallens.presentation.feature.creditstore.unlock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.repository.CreditRepository
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEffect
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEvent
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UnlockConfirmationViewModel(private val creditRepository: CreditRepository,
    private val unlockRepository: com.example.lagallens.domain.repository.AnalysisUnlockRepository? = null) : ViewModel() {
    private val ownerSession = creditRepository.activeSessionKey()
    private val _uiState = MutableStateFlow(UnlockConfirmationUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<UnlockConfirmationUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

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
        val session = ownerSession
        val state = _uiState.value
        val service = unlockRepository
        if (session.isNullOrBlank() || creditRepository.activeSessionKey() != session || state.analysisResultId.isBlank() || state.unlockCost <= 0 || service == null) {
            emitEffect(UnlockConfirmationUiEffect.ShowBackendUnavailable); return
        }
        if (state.isConfirmationRequested || state.isInsufficientCreditsRequested) return
        _uiState.update { it.copy(isConfirmationRequested = true) }
        viewModelScope.launch {
            val outcome = try { service.unlock(session, state.analysisResultId) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { com.example.lagallens.domain.repository.AnalysisUnlockOutcome.Unavailable }
            if (creditRepository.activeSessionKey() != session) return@launch
            when (outcome) {
                is com.example.lagallens.domain.repository.AnalysisUnlockOutcome.Unlocked -> {
                    _uiState.update { it.copy(availableCredits = outcome.balance) }
                    _uiEffect.send(UnlockConfirmationUiEffect.UnlockRequested(state.analysisResultId, state.unlockCost))
                }
                is com.example.lagallens.domain.repository.AnalysisUnlockOutcome.Insufficient -> {
                    _uiState.update { it.copy(availableCredits = outcome.balance, isConfirmationRequested = false, isInsufficientCreditsRequested = true) }
                    _uiEffect.send(UnlockConfirmationUiEffect.NavigateToInsufficientCredits)
                }
                com.example.lagallens.domain.repository.AnalysisUnlockOutcome.Unavailable -> {
                    _uiState.update { it.copy(isConfirmationRequested = false) }
                    _uiEffect.send(UnlockConfirmationUiEffect.ShowBackendUnavailable)
                }
            }
        }
    }

    private fun emitEffect(effect: UnlockConfirmationUiEffect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }

    class Factory(private val repository: CreditRepository, private val unlock: com.example.lagallens.domain.repository.AnalysisUnlockRepository? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = UnlockConfirmationViewModel(repository, unlock) as T
    }
}
