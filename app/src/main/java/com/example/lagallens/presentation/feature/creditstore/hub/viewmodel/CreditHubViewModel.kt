package com.example.lagallens.presentation.feature.creditstore.hub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.creditstore.hub.contract.CreditHubUiEffect
import com.example.lagallens.presentation.feature.creditstore.hub.contract.CreditHubUiEvent
import com.example.lagallens.presentation.feature.creditstore.hub.contract.CreditHubUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreditHubViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CreditHubUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<CreditHubUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

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
}
