package com.example.lagallens.presentation.feature.creditstore.unlock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEffect
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEvent
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UnlockConfirmationViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UnlockConfirmationUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<UnlockConfirmationUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: UnlockConfirmationUiEvent) {
        when (event) {
            is UnlockConfirmationUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    analysisResultId = event.analysisResultId,
                    availableCredits = event.availableCredits.coerceAtLeast(0),
                    unlockCost = event.unlockCost.coerceAtLeast(0),
                    isConfirmationRequested = false,
                    isInsufficientCreditsRequested = false
                )
            }

            UnlockConfirmationUiEvent.BackClicked,
            UnlockConfirmationUiEvent.CancelClicked -> emitEffect(UnlockConfirmationUiEffect.NavigateBack)

            UnlockConfirmationUiEvent.ConfirmClicked -> requestUnlock()
        }
    }

    private fun requestUnlock() {
        val state = _uiState.value
        if (state.isConfirmationRequested || state.isInsufficientCreditsRequested) {
            return
        }
        if (state.availableCredits < state.unlockCost) {
            _uiState.update { it.copy(isInsufficientCreditsRequested = true) }
            emitEffect(UnlockConfirmationUiEffect.NavigateToInsufficientCredits)
            return
        }
        _uiState.update { it.copy(isConfirmationRequested = true) }
        emitEffect(
            UnlockConfirmationUiEffect.UnlockRequested(
                analysisResultId = state.analysisResultId,
                creditCost = state.unlockCost
            )
        )
    }

    private fun emitEffect(effect: UnlockConfirmationUiEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }
}
