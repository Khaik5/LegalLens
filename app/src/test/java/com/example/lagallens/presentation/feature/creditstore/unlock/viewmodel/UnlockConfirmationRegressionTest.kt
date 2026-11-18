package com.example.lagallens.presentation.feature.creditstore.unlock.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelStore
import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.model.credit.CreditReceipt
import com.example.lagallens.domain.model.credit.CreditTransaction
import com.example.lagallens.domain.repository.AnalysisUnlockOutcome
import com.example.lagallens.domain.repository.AnalysisUnlockRepository
import com.example.lagallens.domain.repository.CreditRepository
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEffect
import com.example.lagallens.presentation.feature.creditstore.unlock.contract.UnlockConfirmationUiEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UnlockConfirmationRegressionTest {
    @get:Rule val dispatcher = MainDispatcherRule()
    private val opened = UnlockConfirmationUiEvent.ScreenOpened("result", 5, 2)

    @Test fun transientFailureKeepsCachedPositiveAndZeroBalancesAvailable() = runTest {
        for (balance in listOf(0, 5)) {
            val wallet = Wallet(balance, cached = true).apply { refreshAction = { throw IOException("offline") } }
            val vm = UnlockConfirmationViewModel(wallet, Unlocks())
            try {
                vm.onEvent(opened)
                assertFalse("A cached balance of $balance is valid", vm.uiState.value.isBackendUnavailable)
                assertEquals(balance, vm.uiState.value.availableCredits)
            } finally { vm.viewModelScope.cancel() }
        }
    }

    @Test fun failedInitialLoadWithoutAnyCacheIsUnavailable() = runTest {
        val wallet = Wallet(0, cached = false).apply { refreshAction = { throw IOException("offline") } }
        val vm = UnlockConfirmationViewModel(wallet, Unlocks())
        try {
            vm.onEvent(opened)
            assertTrue(vm.uiState.value.isBackendUnavailable)
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun loadedZeroRemainsAvailableWhenTheNextRefreshFails() = runTest {
        val wallet = Wallet(0, cached = false)
        val vm = UnlockConfirmationViewModel(wallet, Unlocks())
        try {
            vm.onEvent(opened)
            wallet.refreshAction = { throw IOException("offline") }
            vm.onEvent(opened)
            assertEquals(0, vm.uiState.value.availableCredits)
            assertFalse(vm.uiState.value.isBackendUnavailable)
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun reopeningCancelsThePreviousBalanceRefresh() = runTest {
        val wallet = Wallet(5, cached = true)
        var firstCancelled = false
        wallet.refreshAction = {
            if (wallet.refreshCalls == 1) {
                try { awaitCancellation() } finally { firstCancelled = true }
            } else wallet.balance.value = 9
        }
        val vm = UnlockConfirmationViewModel(wallet, Unlocks())
        try {
            vm.onEvent(opened)
            vm.onEvent(opened)
            assertTrue(firstCancelled)
            assertEquals(2, wallet.refreshCalls)
            assertEquals(9, vm.uiState.value.availableCredits)
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun rotationDuringUnlockCannotResetConfirmationOrSendASecondUnlock() = runTest {
        val gate = CompletableDeferred<AnalysisUnlockOutcome>()
        val wallet = Wallet(5, cached = true)
        val unlocks = Unlocks { gate.await() }
        val vm = UnlockConfirmationViewModel(wallet, unlocks)
        try {
            vm.onEvent(opened)
            vm.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
            vm.onEvent(opened)
            assertTrue(vm.uiState.value.isConfirmationRequested)
            vm.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
            assertEquals(1, unlocks.calls)
            assertEquals(1, wallet.refreshCalls)
            gate.complete(AnalysisUnlockOutcome.Unlocked(3))
            assertEquals(UnlockConfirmationUiEffect.UnlockRequested("result", 2), vm.uiEffect.first())
            vm.onEvent(opened)
            vm.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
            assertEquals(1, unlocks.calls)
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun successfulUnlockRefreshesTheSharedWalletForOtherScreens() = runTest {
        val wallet = Wallet(5, cached = true)
        wallet.refreshAction = { if (wallet.refreshCalls > 1) wallet.balance.value = 3 }
        val vm = UnlockConfirmationViewModel(wallet, Unlocks())
        try {
            vm.onEvent(opened)
            vm.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
            assertEquals(UnlockConfirmationUiEffect.UnlockRequested("result", 2), vm.uiEffect.first())
            assertEquals(2, wallet.refreshCalls)
            assertEquals(3, wallet.balance.value)
            assertEquals(3, vm.uiState.value.availableCredits)
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun failedPostUnlockRefreshDoesNotTurnASuccessfulUnlockIntoAnError() = runTest {
        val wallet = Wallet(5, cached = true)
        wallet.refreshAction = { if (wallet.refreshCalls > 1) throw IOException("offline") }
        val vm = UnlockConfirmationViewModel(wallet, Unlocks())
        try {
            vm.onEvent(opened)
            vm.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
            assertEquals(UnlockConfirmationUiEffect.UnlockRequested("result", 2), vm.uiEffect.first())
            assertEquals(2, wallet.refreshCalls)
            assertEquals(3, vm.uiState.value.availableCredits)
            assertFalse(vm.uiState.value.isBackendUnavailable)
            assertTrue(vm.uiState.value.isConfirmationRequested)
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun slowPostUnlockRefreshDoesNotBlockTheSuccessEffect() = runTest {
        val wallet = Wallet(5, cached = true)
        wallet.refreshAction = { if (wallet.refreshCalls > 1) awaitCancellation() }
        val vm = UnlockConfirmationViewModel(wallet, Unlocks())
        try {
            vm.onEvent(opened)
            vm.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
            assertEquals(2, wallet.refreshCalls)
            assertEquals(UnlockConfirmationUiEffect.UnlockRequested("result", 2), vm.uiEffect.first())
            assertEquals(3, vm.uiState.value.availableCredits)
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun clearingViewModelAfterSuccessDoesNotCancelSharedWalletRefresh() = runTest {
        val gate = CompletableDeferred<Unit>()
        val wallet = Wallet(5, cached = true)
        wallet.refreshAction = {
            if (wallet.refreshCalls > 1) {
                gate.await()
                wallet.balance.value = 3
            }
        }
        val store = ViewModelStore()
        val vm = UnlockConfirmationViewModel(wallet, Unlocks())
        store.put("unlock", vm)
        try {
            vm.onEvent(opened)
            vm.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
            assertEquals(UnlockConfirmationUiEffect.UnlockRequested("result", 2), vm.uiEffect.first())
            assertEquals(2, wallet.refreshCalls)
            assertEquals(5, wallet.balance.value)
            store.clear()
            gate.complete(Unit)
            runCurrent()
            assertEquals("Hub and Store must receive the refreshed wallet even after this screen closes", 3, wallet.balance.value)
        } finally {
            gate.complete(Unit)
            store.clear()
            runCurrent()
        }
    }

    @Test fun postUnlockRefreshStillTimesOutAfterTheViewModelIsCleared() = runTest {
        val wallet = Wallet(5, cached = true)
        var refreshStopped = false
        wallet.refreshAction = {
            if (wallet.refreshCalls > 1) {
                try { awaitCancellation() } finally { refreshStopped = true }
            }
        }
        val store = ViewModelStore()
        val vm = UnlockConfirmationViewModel(wallet, Unlocks())
        val scopeJob = requireNotNull(vm.viewModelScope.coroutineContext[Job])
        store.put("unlock", vm)
        try {
            vm.onEvent(opened)
            vm.onEvent(UnlockConfirmationUiEvent.ConfirmClicked)
            assertEquals(UnlockConfirmationUiEffect.UnlockRequested("result", 2), vm.uiEffect.first())
            store.clear()
            advanceTimeBy(11_000)
            runCurrent()
            assertTrue("A stalled refresh must not remain alive indefinitely", refreshStopped)
            assertTrue(scopeJob.isCompleted)
            assertEquals(3, vm.uiState.value.availableCredits)
            assertFalse(vm.uiState.value.isBackendUnavailable)
        } finally {
            store.clear()
            advanceTimeBy(11_000)
            runCurrent()
        }
    }

    private class Wallet(initial: Int, var cached: Boolean) : CreditRepository {
        override val balance = MutableStateFlow(initial)
        override val hasCachedBalance get() = cached
        override fun activeSessionKey() = "owner"
        var refreshCalls = 0
        var refreshAction: suspend () -> Unit = {}
        override suspend fun refresh() { refreshCalls++; refreshAction(); cached = true }
        override suspend fun getTransactions(): List<CreditTransaction> = emptyList()
        override suspend fun spend(transactionId: String, credits: Int): CreditReceipt = error("No direct spending")
    }

    private class Unlocks(private val action: suspend () -> AnalysisUnlockOutcome = { AnalysisUnlockOutcome.Unlocked(3) }) : AnalysisUnlockRepository {
        var calls = 0
        override suspend fun status(sessionKey: String, resultId: String) = error("unused")
        override suspend fun unlock(sessionKey: String, resultId: String): AnalysisUnlockOutcome { calls++; return action() }
    }
}
