package com.example.lagallens.presentation.feature.creditstore.hub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import com.example.lagallens.domain.repository.CreditRepository
import kotlinx.coroutines.flow.update
import com.example.lagallens.presentation.feature.creditstore.hub.contract.CreditHubUiEffect
import com.example.lagallens.presentation.feature.creditstore.hub.contract.CreditHubUiEvent
import com.example.lagallens.presentation.feature.creditstore.hub.contract.CreditHubUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreditHubViewModel(private val creditRepository: CreditRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CreditHubUiState(
        balanceCredits = creditRepository.balance.value,
        isBackendUnavailable = !creditRepository.isConfigured
    ))
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<CreditHubUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init { viewModelScope.launch { creditRepository.balance.collect { balance -> _uiState.update { it.copy(balanceCredits = balance, isBackendUnavailable = !creditRepository.isConfigured) } } } }

    fun onEvent(event: CreditHubUiEvent) {
        when (event) {
            CreditHubUiEvent.BackClicked -> emitEffect(CreditHubUiEffect.NavigateBack)
            CreditHubUiEvent.BuyCreditsClicked -> emitEffect(CreditHubUiEffect.NavigateToStore)
            CreditHubUiEvent.HistoryClicked -> emitEffect(CreditHubUiEffect.NavigateToHistory)
            CreditHubUiEvent.UnlockResultsClicked -> emitEffect(CreditHubUiEffect.NavigateToUnlockConfirmation)
        }
    }

    private fun emitEffect(effect: CreditHubUiEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }

    class Factory(private val repository: CreditRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CreditHubViewModel(repository) as T
    }
}
