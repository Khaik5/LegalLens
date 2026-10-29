package com.example.lagallens.presentation.feature.creditstore.flow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.repository.PaymentRepository
import com.example.lagallens.domain.model.payment.PaymentStatus
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreditFlowViewModel(private val paymentRepository: PaymentRepository) : ViewModel() {
    private val ownerSession = paymentRepository.activeSessionKey()
    private val _uiState = MutableStateFlow(CreditFlowUiState(isBackendUnavailable = !paymentRepository.isConfigured))
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<CreditFlowUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()
    private var activePaymentId: String? = null
    private var activePaymentSession: String? = null

    fun onEvent(event: CreditFlowUiEvent) {
        if ((event is CreditFlowUiEvent.StartPayment || event is CreditFlowUiEvent.CompletePayment || event == CreditFlowUiEvent.RequestPaymentClicked)
            && paymentRepository.activeSessionKey() != ownerSession) return
        when (event) {
            CreditFlowUiEvent.BackClicked -> {
                val session = activePaymentSession
                activePaymentId?.let { id -> viewModelScope.launch {
                    if (paymentRepository.activeSessionKey() == session) runCatching { paymentRepository.updateStatus(id, PaymentStatus.CANCELLED) }
                } }
                emitEffect(CreditFlowUiEffect.NavigateBack)
            }
            CreditFlowUiEvent.ContinueFreeClicked -> emitEffect(CreditFlowUiEffect.NavigateBack)

            CreditFlowUiEvent.OpenCreditStoreClicked -> emitEffect(CreditFlowUiEffect.NavigateToCreditStore)
            CreditFlowUiEvent.OpenCreditHistoryClicked -> emitEffect(CreditFlowUiEffect.NavigateToCreditHistory)
            CreditFlowUiEvent.OpenAnalysisHistoryClicked -> emitEffect(CreditFlowUiEffect.ReturnToAnalysisHistory)
            CreditFlowUiEvent.ReturnToUnlockClicked -> emitEffect(CreditFlowUiEffect.ReturnToUnlockConfirmation)
            is CreditFlowUiEvent.OpenUnlockedResultClicked -> emitEffect(
                CreditFlowUiEffect.ReturnUnlockedResult(event.analysisResultId)
            )

            CreditFlowUiEvent.RequestPaymentClicked -> requestPayment()
            is CreditFlowUiEvent.StartPayment -> startPayment(event)
            is CreditFlowUiEvent.CompletePayment -> completePayment(event.paymentId)
        }
    }

    private fun requestPayment() {
        if (!paymentRepository.isConfigured) {
            emitEffect(CreditFlowUiEffect.PaymentFailed)
            return
        }
        if (_uiState.value.isPaymentRequesting) {
            return
        }
        _uiState.update { it.copy(isPaymentRequesting = true) }
        emitEffect(CreditFlowUiEffect.PaymentRequested)
    }

    private fun startPayment(event: CreditFlowUiEvent.StartPayment) {
        val repository = paymentRepository
        activePaymentId = event.paymentId
        val session = repository.activeSessionKey()
        activePaymentSession = session
        viewModelScope.launch {
            if (repository.activeSessionKey() != session) return@launch
            runCatching { repository.createOrGet(event.paymentId, event.packageId, event.amountVnd, event.credits) }
                .onFailure { emitEffect(CreditFlowUiEffect.PaymentFailed, session) }
        }
    }

    private fun completePayment(paymentId: String) {
        val repository = paymentRepository
        if (paymentId != activePaymentId) return
        val session = activePaymentSession
        viewModelScope.launch {
            if (repository.activeSessionKey() != session) return@launch
            runCatching { repository.completeDemoPayment(paymentId) }
                .onSuccess { payment -> if (repository.activeSessionKey() == session) { if (payment.status == PaymentStatus.SUCCESS) emitEffect(CreditFlowUiEffect.PaymentSucceeded(payment), session) else emitEffect(CreditFlowUiEffect.PaymentFailed, session) } }
                .onFailure { emitEffect(CreditFlowUiEffect.PaymentFailed, session) }
        }
    }

    private fun emitEffect(effect: CreditFlowUiEffect, session: String? = paymentRepository.activeSessionKey()) {
        viewModelScope.launch {
            if (paymentRepository.activeSessionKey() == session) _uiEffect.emit(effect)
        }
    }

    class Factory(private val repository: PaymentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CreditFlowViewModel(repository) as T
    }
}
