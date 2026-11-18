package com.example.lagallens.presentation.feature.creditstore.hub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.example.lagallens.domain.repository.CreditRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import com.example.lagallens.presentation.feature.creditstore.hub.contract.CreditHubUiEffect
import com.example.lagallens.presentation.feature.creditstore.hub.contract.CreditHubUiEvent
import com.example.lagallens.presentation.feature.creditstore.hub.contract.CreditHubUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class CreditHubViewModel(private val creditRepository: CreditRepository) : ViewModel() {
    private val ownerSession = creditRepository.activeSessionKey()
    fun isOwnerSession() = ownerSession == creditRepository.activeSessionKey()
    private val _uiState = MutableStateFlow(CreditHubUiState(
        balanceCredits = creditRepository.balance.value,
        isBackendUnavailable = !creditRepository.isConfigured
    ))
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<CreditHubUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var balanceJob: Job? = null

    init {
        viewModelScope.launch {
            creditRepository.balance.collect { balance ->
                _uiState.update { it.copy(balanceCredits = if (isOwnerSession()) balance else 0, isBackendUnavailable = !creditRepository.isConfigured) }
            }
        }
        viewModelScope.launch {
            creditRepository.observeSessionKeys().collect {
                if (it != ownerSession) _uiState.value = CreditHubUiState(balanceCredits = 0, isBackendUnavailable = !creditRepository.isConfigured)
            }
        }
        refreshBalance()
    }

    private fun refreshBalance() {
        balanceJob?.cancel()
        if (!creditRepository.isConfigured || !isOwnerSession()) return
        balanceJob = viewModelScope.launch {
            try {
                creditRepository.refresh()
                if (isOwnerSession()) _uiState.update { it.copy(isBalanceUnavailable = false) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                if (isOwnerSession()) _uiState.update { it.copy(isBalanceUnavailable = true) }
            }
        }
    }

    fun onEvent(event: CreditHubUiEvent) {
        if (!isOwnerSession()) return
        when (event) {
            CreditHubUiEvent.RetryClicked -> refreshBalance()
            CreditHubUiEvent.BackClicked -> emitEffect(CreditHubUiEffect.NavigateBack)
            CreditHubUiEvent.BuyCreditsClicked -> emitEffect(CreditHubUiEffect.NavigateToStore)
            CreditHubUiEvent.HistoryClicked -> emitEffect(CreditHubUiEffect.NavigateToHistory)
            CreditHubUiEvent.PaymentsClicked -> emitEffect(CreditHubUiEffect.NavigateToPayments)
            CreditHubUiEvent.UnlockResultsClicked -> emitEffect(CreditHubUiEffect.NavigateToUnlockConfirmation)
        }
    }

    private fun emitEffect(effect: CreditHubUiEffect) {
        viewModelScope.launch {
            if (isOwnerSession()) _uiEffect.send(effect)
        }
    }

    class Factory(private val repository: CreditRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CreditHubViewModel(repository) as T
    }
}
