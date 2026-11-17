package com.example.lagallens.presentation.feature.creditstore.payments.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.repository.PaymentRepository
import com.example.lagallens.presentation.feature.creditstore.payments.contract.PaymentHistoryUiEffect
import com.example.lagallens.presentation.feature.creditstore.payments.contract.PaymentHistoryUiEvent
import com.example.lagallens.presentation.feature.creditstore.payments.contract.PaymentHistoryUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaymentHistoryViewModel(private val payments: PaymentRepository) : ViewModel() {
    private val ownerSession = payments.activeSessionKey()
    private val _uiState = MutableStateFlow(PaymentHistoryUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<PaymentHistoryUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var loadJob: Job? = null

    fun isOwnerSession() = ownerSession != null && ownerSession == payments.activeSessionKey()

    fun onEvent(event: PaymentHistoryUiEvent) {
        when (event) {
            PaymentHistoryUiEvent.ScreenOpened -> if (loadJob == null) load()
            PaymentHistoryUiEvent.RetryClicked -> load()
            PaymentHistoryUiEvent.BackClicked -> _uiEffect.trySend(PaymentHistoryUiEffect.NavigateBack)
        }
    }

    private fun load() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
        loadJob = viewModelScope.launch {
            try {
                val entries = payments.getHistory()
                if (!isOwnerSession()) return@launch
                _uiState.update { it.copy(isLoading = false, entries = entries) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                if (isOwnerSession()) {
                    _uiState.update { it.copy(isLoading = false, entries = emptyList(), errorMessageRes = R.string.payment_history_error) }
                }
            }
        }
    }

    class Factory(private val payments: PaymentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PaymentHistoryViewModel(payments) as T
    }
}
