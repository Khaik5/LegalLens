package com.example.lagallens.presentation.feature.creditstore.flow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreditFlowViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CreditFlowUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<CreditFlowUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: CreditFlowUiEvent) {
        when (event) {
            CreditFlowUiEvent.BackClicked,
            CreditFlowUiEvent.ContinueFreeClicked -> emitEffect(CreditFlowUiEffect.NavigateBack)

            CreditFlowUiEvent.OpenCreditStoreClicked -> emitEffect(CreditFlowUiEffect.NavigateToCreditStore)
            CreditFlowUiEvent.OpenCreditHistoryClicked -> emitEffect(CreditFlowUiEffect.NavigateToCreditHistory)
            CreditFlowUiEvent.OpenAnalysisHistoryClicked -> emitEffect(CreditFlowUiEffect.ReturnToAnalysisHistory)
            CreditFlowUiEvent.ReturnToUnlockClicked -> emitEffect(CreditFlowUiEffect.ReturnToUnlockConfirmation)
            is CreditFlowUiEvent.OpenUnlockedResultClicked -> emitEffect(
                CreditFlowUiEffect.ReturnUnlockedResult(event.analysisResultId)
            )

            CreditFlowUiEvent.RequestPaymentClicked -> requestPayment()
        }
    }

    private fun requestPayment() {
        if (_uiState.value.isPaymentRequesting) {
            return
        }
        _uiState.update { it.copy(isPaymentRequesting = true) }
        emitEffect(CreditFlowUiEffect.PaymentRequested)
    }

    private fun emitEffect(effect: CreditFlowUiEffect) {
        viewModelScope.launch {
            _uiEffect.emit(effect)
        }
    }
}
