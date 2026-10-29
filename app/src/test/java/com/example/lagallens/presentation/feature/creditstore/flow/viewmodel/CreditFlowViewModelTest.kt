package com.example.lagallens.presentation.feature.creditstore.flow.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.model.payment.PaymentRecord
import com.example.lagallens.domain.model.payment.PaymentStatus
import com.example.lagallens.domain.repository.PaymentRepository
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEffect
import com.example.lagallens.presentation.feature.creditstore.flow.contract.CreditFlowUiEvent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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
        val repository = object : PaymentRepository by TestPaymentRepository {
            override fun activeSessionKey() = session
            override suspend fun createOrGet(paymentId: String, packageId: String, amountVnd: Long, credits: Int): PaymentRecord {
                creates++
                return TestPaymentRepository.createOrGet(paymentId, packageId, amountVnd, credits)
            }
        }
        val viewModel = CreditFlowViewModel(repository)
        session = "B:2"
        viewModel.onEvent(CreditFlowUiEvent.StartPayment("p", "pack", 1000, 3))
        viewModel.onEvent(CreditFlowUiEvent.RequestPaymentClicked)
        assertEquals(0, creates)
        assertEquals(false, viewModel.uiState.value.isPaymentRequesting)
    }

    @Test
    fun paymentRequestPreventsDuplicateSubmissionAndEmitsOneEffect() = runTest {
        val viewModel = CreditFlowViewModel(TestPaymentRepository)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEffect.first() }

        viewModel.onEvent(CreditFlowUiEvent.RequestPaymentClicked)
        viewModel.onEvent(CreditFlowUiEvent.RequestPaymentClicked)

        assertTrue(viewModel.uiState.value.isPaymentRequesting)
        assertEquals(CreditFlowUiEffect.PaymentRequested, effect.await())
    }

    private object TestPaymentRepository : PaymentRepository {
        override suspend fun createOrGet(paymentId: String, packageId: String, amountVnd: Long, credits: Int) =
            PaymentRecord(paymentId, packageId, amountVnd, credits, PaymentStatus.PENDING, 0)
        override suspend fun updateStatus(paymentId: String, status: PaymentStatus) =
            PaymentRecord(paymentId, "test", 0, 1, status, 0)
        override suspend fun get(paymentId: String): PaymentRecord? = null
        override suspend fun completeDemoPayment(paymentId: String) =
            PaymentRecord(paymentId, "test", 0, 1, PaymentStatus.SUCCESS, 0)
    }
}
