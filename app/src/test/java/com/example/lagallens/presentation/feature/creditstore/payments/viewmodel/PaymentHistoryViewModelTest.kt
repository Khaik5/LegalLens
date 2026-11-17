package com.example.lagallens.presentation.feature.creditstore.payments.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.R
import com.example.lagallens.domain.model.credit.CreditOffer
import com.example.lagallens.domain.model.payment.PaymentHistoryEntry
import com.example.lagallens.domain.model.payment.PaymentRecord
import com.example.lagallens.domain.model.payment.PaymentStatus
import com.example.lagallens.domain.repository.PaymentRepository
import com.example.lagallens.presentation.feature.creditstore.payments.contract.PaymentHistoryUiEffect
import com.example.lagallens.presentation.feature.creditstore.payments.contract.PaymentHistoryUiEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class PaymentHistoryViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private class Payments : PaymentRepository {
        var session: String? = "account:1"
        var history: List<PaymentHistoryEntry> = emptyList()
        var failure: Exception? = null
        var loads = 0

        override fun activeSessionKey() = session
        override suspend fun getPackages(): List<CreditOffer> = emptyList()
        override suspend fun createPayment(paymentId: String, packageId: String): PaymentRecord = error("unused")
        override fun observePayment(paymentId: String): Flow<PaymentRecord> = emptyFlow()
        override suspend fun cancelPayment(paymentId: String): PaymentRecord = error("unused")
        override suspend fun get(paymentId: String): PaymentRecord? = null
        override suspend fun getHistory(): List<PaymentHistoryEntry> {
            loads++
            failure?.let { throw it }
            return history
        }
    }

    private val payments = Payments()
    private val entry = PaymentHistoryEntry("5", 15, 79_000, PaymentStatus.SUCCESS, 1L)

    @Test fun openingTheScreenLoadsTheHistoryOnce() = runTest {
        payments.history = listOf(entry)
        val viewModel = PaymentHistoryViewModel(payments)

        viewModel.onEvent(PaymentHistoryUiEvent.ScreenOpened)
        viewModel.onEvent(PaymentHistoryUiEvent.ScreenOpened)

        assertEquals(1, payments.loads)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(entry), viewModel.uiState.value.entries)
    }

    @Test fun aFailedLoadShowsAnErrorAndRetryRecovers() = runTest {
        payments.failure = IllegalStateException("offline")
        val viewModel = PaymentHistoryViewModel(payments)
        viewModel.onEvent(PaymentHistoryUiEvent.ScreenOpened)
        assertEquals(R.string.payment_history_error, viewModel.uiState.value.errorMessageRes)

        payments.failure = null
        payments.history = listOf(entry)
        viewModel.onEvent(PaymentHistoryUiEvent.RetryClicked)

        assertNull(viewModel.uiState.value.errorMessageRes)
        assertEquals(listOf(entry), viewModel.uiState.value.entries)
    }

    @Test fun aResultForAnotherSessionIsDropped() = runTest {
        payments.history = listOf(entry)
        val viewModel = PaymentHistoryViewModel(payments)
        payments.session = "account:2"

        viewModel.onEvent(PaymentHistoryUiEvent.ScreenOpened)

        assertEquals(emptyList<PaymentHistoryEntry>(), viewModel.uiState.value.entries)
    }

    @Test fun backNavigatesBack() = runTest {
        val viewModel = PaymentHistoryViewModel(payments)

        viewModel.onEvent(PaymentHistoryUiEvent.BackClicked)

        assertEquals(PaymentHistoryUiEffect.NavigateBack, viewModel.uiEffect.first())
    }
}
