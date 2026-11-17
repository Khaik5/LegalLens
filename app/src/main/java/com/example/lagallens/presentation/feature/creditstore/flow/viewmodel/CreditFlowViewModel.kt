package com.example.lagallens.presentation.feature.creditstore.flow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.repository.PaymentRepository
import com.example.lagallens.domain.model.payment.BankTransfer
import com.example.lagallens.domain.model.payment.BankTransferUnavailableException
import com.example.lagallens.domain.model.payment.PaymentRecord
import com.example.lagallens.domain.model.payment.PaymentStatus
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class CreditFlowViewModel(private val paymentRepository: PaymentRepository) : ViewModel() {
    private val ownerSession = paymentRepository.activeSessionKey()
    private val _uiState = MutableStateFlow(CreditFlowUiState(isBackendUnavailable = !paymentRepository.isConfigured))
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<CreditFlowUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()
    private var paymentJob: Job? = null
    private var activePaymentId: String? = null
    private var activePaymentSession: String? = null

    fun onEvent(event: CreditFlowUiEvent) {
        if ((event is CreditFlowUiEvent.StartPayment || event == CreditFlowUiEvent.RequestPaymentClicked)
            && paymentRepository.activeSessionKey() != ownerSession) return
        when (event) {
            CreditFlowUiEvent.BackClicked -> {
                val session = activePaymentSession
                paymentJob?.cancel()
                activePaymentId?.let { id -> viewModelScope.launch {
                    if (paymentRepository.activeSessionKey() == session) runCatching { paymentRepository.cancelPayment(id) }
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
        // The screen restarts on rotation while this ViewModel and its payment stay alive.
        if (activePaymentId == event.paymentId) return
        val repository = paymentRepository
        activePaymentId = event.paymentId
        val session = repository.activeSessionKey()
        activePaymentSession = session
        paymentJob = viewModelScope.launch {
            if (repository.activeSessionKey() != session) return@launch
            val finalEffect = try {
                repository.createPayment(event.paymentId, event.packageId)
                val transfer = loadBankTransfer(repository, event.paymentId, session)
                val payment = awaitPayment(repository, event.paymentId, transfer?.expiresAtEpochMillis)
                if (payment?.status == PaymentStatus.SUCCESS) CreditFlowUiEffect.PaymentSucceeded(payment) else CreditFlowUiEffect.PaymentFailed
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                CreditFlowUiEffect.PaymentFailed
            }
            emitEffect(finalEffect, session)
        }
    }

    /** Shows the transfer code when the data source has one; a missing code never stops the payment from being tracked. */
    private suspend fun loadBankTransfer(repository: PaymentRepository, paymentId: String, session: String?): BankTransfer? {
        val transfer = try {
            repository.getBankTransfer(paymentId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: BankTransferUnavailableException) {
            if (repository.activeSessionKey() == session) _uiState.update { it.copy(isBankTransferUnavailable = true) }
            return null
        }
        if (transfer != null && repository.activeSessionKey() == session) _uiState.update { it.copy(bankTransfer = transfer) }
        return transfer
    }

    /** Waits for a final status; an unpaid transfer is cancelled when its time runs out. Null means it expired. */
    private suspend fun awaitPayment(repository: PaymentRepository, paymentId: String, expiresAtEpochMillis: Long?): PaymentRecord? {
        val wait = suspend { repository.observePayment(paymentId).first { it.status != PaymentStatus.PENDING } }
        if (expiresAtEpochMillis == null) return wait()
        val remaining = expiresAtEpochMillis - System.currentTimeMillis() + EXPIRY_GRACE_MILLIS
        val payment = withTimeoutOrNull(remaining.coerceAtLeast(0L)) { wait() }
        if (payment == null) runCatching { repository.cancelPayment(paymentId) }
        return payment
    }

    private fun emitEffect(effect: CreditFlowUiEffect, session: String? = paymentRepository.activeSessionKey()) {
        viewModelScope.launch {
            if (paymentRepository.activeSessionKey() == session) _uiEffect.send(effect)
        }
    }

    private companion object {
        /** The backend may confirm a transfer shortly after the code expires. */
        const val EXPIRY_GRACE_MILLIS = 30_000L
    }

    class Factory(private val repository: PaymentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CreditFlowViewModel(repository) as T
    }
}
