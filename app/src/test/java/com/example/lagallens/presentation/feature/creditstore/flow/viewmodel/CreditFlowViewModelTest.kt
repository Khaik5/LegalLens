package com.example.lagallens.presentation.feature.creditstore.flow.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.model.payment.BankTransfer
import com.example.lagallens.domain.model.payment.PaymentRecord
import com.example.lagallens.domain.model.payment.PaymentStatus
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CreditFlowViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun oldScreenCannotStartPaymentForNewSession() = runTest {
        var session = "A:1"
        var creates = 0
        val repository = object : com.example.lagallens.TestPaymentRepository() {
            override fun activeSessionKey() = session
            override suspend fun createPayment(paymentId: String, packageId: String): PaymentRecord {
                creates++
                return super.createPayment(paymentId, packageId)
            }
        }
        val viewModel = CreditFlowViewModel(repository)
        session = "B:2"
        viewModel.onEvent(CreditFlowUiEvent.StartPayment("p", "pack"))
        viewModel.onEvent(CreditFlowUiEvent.RequestPaymentClicked)
        assertEquals(0, creates)
        assertEquals(false, viewModel.uiState.value.isPaymentRequesting)
    }

    private fun record(status: PaymentStatus) = PaymentRecord("p", "pack", 29_000, 5, status, 0)

    @Test fun aFailedStatusCallDoesNotMakeThePaymentFail() = runTest {
        var calls = 0
        val repository = object : com.example.lagallens.TestPaymentRepository() {
            override fun observePayment(paymentId: String): Flow<PaymentRecord> = flow {
                if (calls++ < 2) throw java.io.IOException("gateway")
                emit(record(PaymentStatus.SUCCESS))
            }
        }
        val viewModel = CreditFlowViewModel(repository)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEffect.first() }

        viewModel.onEvent(CreditFlowUiEvent.StartPayment("p", "pack"))
        advanceUntilIdle()

        assertTrue(effect.await() is CreditFlowUiEffect.PaymentSucceeded)
        assertEquals(3, calls)
    }

    @Test fun statusCallsThatNeverRecoverEndWithFailureAndCancelWhenTheWindowCloses() = runTest {
        var cancels = 0
        val repository = object : com.example.lagallens.TestPaymentRepository() {
            override fun observePayment(paymentId: String): Flow<PaymentRecord> = flow { throw java.io.IOException("down") }
            override suspend fun cancelPayment(paymentId: String): PaymentRecord {
                cancels++
                return super.cancelPayment(paymentId)
            }
        }
        val viewModel = CreditFlowViewModel(repository)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEffect.first() }

        viewModel.onEvent(CreditFlowUiEvent.StartPayment("p", "pack"))
        advanceUntilIdle()

        assertEquals(CreditFlowUiEffect.PaymentFailed, effect.await())
        assertEquals(1, cancels)
    }

    @Test fun aTransferPaidAtTheLastMomentIsReportedAsSuccess() = runTest {
        // The expiry is long past, so the window is closed when the wait starts. The transfer was paid after the
        // last status the app saw: cancelling is refused (409) and the status read afterwards says SUCCESS.
        val repository = object : com.example.lagallens.TestPaymentRepository() {
            override fun observePayment(paymentId: String): Flow<PaymentRecord> = flow { emit(record(PaymentStatus.SUCCESS)) }
            override suspend fun cancelPayment(paymentId: String): PaymentRecord =
                throw IllegalStateException("409 PAYMENT_INVALID_STATUS")
            override suspend fun getBankTransfer(paymentId: String) = BankTransfer(paymentId, 29_000, "LL1", "qr", 0L)
        }
        val viewModel = CreditFlowViewModel(repository)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEffect.first() }

        viewModel.onEvent(CreditFlowUiEvent.StartPayment("p", "pack"))
        advanceUntilIdle()

        assertTrue(effect.await() is CreditFlowUiEffect.PaymentSucceeded)
    }

    @Test fun backCancelsThePendingPaymentOnce() = runTest {
        var cancels = 0
        val repository = object : com.example.lagallens.TestPaymentRepository() {
            override fun observePayment(paymentId: String): Flow<PaymentRecord> = flow { awaitCancellation() }
            override suspend fun cancelPayment(paymentId: String): PaymentRecord {
                cancels++
                return super.cancelPayment(paymentId)
            }
        }
        val viewModel = CreditFlowViewModel(repository)
        viewModel.onEvent(CreditFlowUiEvent.StartPayment("p", "pack"))

        viewModel.onEvent(CreditFlowUiEvent.BackClicked)
        advanceUntilIdle()

        assertEquals(1, cancels)
    }

    @Test
    fun paymentRequestPreventsDuplicateSubmissionAndEmitsOneEffect() = runTest {
        val viewModel = CreditFlowViewModel(com.example.lagallens.TestPaymentRepository())
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEffect.first() }

        viewModel.onEvent(CreditFlowUiEvent.RequestPaymentClicked)
        viewModel.onEvent(CreditFlowUiEvent.RequestPaymentClicked)

        assertTrue(viewModel.uiState.value.isPaymentRequesting)
        assertEquals(CreditFlowUiEffect.PaymentRequested, effect.await())
    }
}
