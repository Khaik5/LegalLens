package com.example.lagallens.presentation.feature.creditstore.flow.viewmodel

import com.example.lagallens.MainDispatcherRule
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

    @Test
    fun paymentRequestPreventsDuplicateSubmissionAndEmitsOneEffect() = runTest {
        val viewModel = CreditFlowViewModel()
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEffect.first() }

        viewModel.onEvent(CreditFlowUiEvent.RequestPaymentClicked)
        viewModel.onEvent(CreditFlowUiEvent.RequestPaymentClicked)

        assertTrue(viewModel.uiState.value.isPaymentRequesting)
        assertEquals(CreditFlowUiEffect.PaymentRequested, effect.await())
    }
}
