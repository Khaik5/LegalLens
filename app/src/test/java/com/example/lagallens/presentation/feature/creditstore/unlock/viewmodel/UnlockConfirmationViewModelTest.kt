package com.example.lagallens.presentation.feature.creditstore.unlock.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.data.datasource.local.datastore.CreditDemoLedger
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEffect
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEvent
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UnlockConfirmationViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun missingValidatedEntitlementCannotDebitEvenWithPositiveBalance() = runTest {
        val ledger = CreditDemoLedger(initialBalance = 5)
        val viewModel = UnlockConfirmationViewModel(ledger)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEffect.first() }
        viewModel.onEvent(UnlockConfirmationUiEvent.ScreenOpened("unverified-result", 5, 2))
        viewModel.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
        assertEquals(UnlockConfirmationUiEffect.ShowBackendUnavailable, effect.await())
        assertEquals(5, ledger.balance.value)
    }

    @Test fun repositoryFailureNeverEmitsUnlockSuccessAndAllowsRetry() = runTest {
        val ledger = object : CreditDemoLedger(initialBalance = 5) { override fun activeSessionKey() = "a:session" }
        val unlock = object : com.example.lagallens.TestUnlockRepository() {
            override suspend fun unlock(sessionKey: String, resultId: String): com.example.lagallens.domain.repository.AnalysisUnlockOutcome = throw java.io.IOException("write failed")
        }
        val vm = UnlockConfirmationViewModel(ledger, unlock)
        vm.onEvent(UnlockConfirmationUiEvent.ScreenOpened("result", 5, 2))
        vm.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
        assertEquals(UnlockConfirmationUiEffect.ShowBackendUnavailable, vm.uiEffect.first())
        org.junit.Assert.assertFalse(vm.uiState.value.isConfirmationRequested)
        assertEquals(5, ledger.balance.value)
    }

    @Test fun missingResultCannotDebit() = runTest {
        val ledger = CreditDemoLedger(initialBalance = 5)
        val viewModel = UnlockConfirmationViewModel(ledger)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEffect.first() }
        viewModel.onEvent(UnlockConfirmationUiEvent.ScreenOpened("", 5, 2))
        viewModel.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
        assertEquals(UnlockConfirmationUiEffect.ShowBackendUnavailable, effect.await())
        assertEquals(5, ledger.balance.value)
    }

    @Test fun validatedUnlockCarriesSameResultAndOwner() = runTest {
        val ledger = object : CreditDemoLedger(initialBalance = 5) { override fun activeSessionKey() = "account-a:session" }
        var request = ""
        val unlock = object : com.example.lagallens.TestUnlockRepository() {
            override suspend fun unlock(sessionKey: String, resultId: String): com.example.lagallens.domain.repository.AnalysisUnlockOutcome {
                request = "$sessionKey/$resultId"
                return com.example.lagallens.domain.repository.AnalysisUnlockOutcome.Unlocked(3)
            }
        }
        val viewModel = UnlockConfirmationViewModel(ledger, unlock)
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.uiEffect.first() }
        viewModel.onEvent(UnlockConfirmationUiEvent.ScreenOpened("result-a", 5, 2))
        viewModel.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
        assertEquals("account-a:session/result-a", request)
        assertEquals(UnlockConfirmationUiEffect.UnlockRequested("result-a", 2), effect.await())
    }
}
