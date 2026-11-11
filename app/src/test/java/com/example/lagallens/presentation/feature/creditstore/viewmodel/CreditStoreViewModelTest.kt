package com.example.lagallens.presentation.feature.creditstore.viewmodel

import com.example.lagallens.data.datasource.local.datastore.CreditDemoLedger
import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiEffect
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiEvent
import com.example.lagallens.domain.repository.CreditRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.lagallens.presentation.feature.creditstore.hub.viewmodel.CreditHubViewModel
import com.example.lagallens.presentation.feature.creditstore.hub.contract.CreditHubUiEvent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CreditStoreViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun topUpUsesSelectedPackageForEveryOption() = runTest {
        val model = CreditStoreViewModel(CreditDemoLedger(initialBalance = 5), com.example.lagallens.TestPaymentRepository())
        listOf("credits_5", "credits_15", "credits_30", "credits_100").forEach { id ->
            model.onEvent(CreditStoreUiEvent.PackageClicked(id))
            val effect = async { model.uiEffect.first() }
            kotlinx.coroutines.yield()
            model.onEvent(CreditStoreUiEvent.TopUpClicked)
            val navigation = effect.await() as CreditStoreUiEffect.NavigateToPurchaseConfirmation
            assertEquals(id, navigation.creditPackage.id)
            assertEquals(5, navigation.balanceCredits)
        }
    }

    @Test fun recommendedFlagComesFromThePackageAndStartsSelected() = runTest {
        val model = CreditStoreViewModel(CreditDemoLedger(initialBalance = 5), com.example.lagallens.TestPaymentRepository())

        val packages = model.uiState.value.packages

        assertEquals(listOf("credits_15"), packages.filter { it.isRecommended }.map { it.id })
        assertEquals(listOf("credits_15"), packages.filter { it.isSelected }.map { it.id })
        model.onEvent(CreditStoreUiEvent.PackageClicked("credits_30"))
        assertEquals(listOf("credits_15"), model.uiState.value.packages.filter { it.isRecommended }.map { it.id })
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun queuedNavigationFromOldGenerationIsDiscarded() = runTest {
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        var session = "a:1"
        val repository = object : CreditRepository by CreditDemoLedger(initialBalance = 5) {
            override fun activeSessionKey() = session
        }
        val model = CreditStoreViewModel(repository, com.example.lagallens.TestPaymentRepository())
        val effects = mutableListOf<CreditStoreUiEffect>()
        backgroundScope.launch { model.uiEffect.collect { effects.add(it) } }
        testScheduler.runCurrent()
        model.onEvent(CreditStoreUiEvent.ViewHistoryClicked)
        session = "b:2"
        testScheduler.runCurrent()
        session = "a:3"
        model.onEvent(CreditStoreUiEvent.ViewHistoryClicked)
        testScheduler.runCurrent()
        assertEquals(emptyList<CreditStoreUiEffect>(), effects)
    }

    @Test fun sessionChangeClearsBothScreensEvenWhenBalanceDoesNotChange() = runTest {
        val session = MutableStateFlow<String?>("a:1")
        val repository = object : CreditRepository by CreditDemoLedger(initialBalance = 5) {
            override fun activeSessionKey() = session.value
            override fun observeSessionKeys() = session
        }
        val store = CreditStoreViewModel(repository, com.example.lagallens.TestPaymentRepository())
        val hub = CreditHubViewModel(repository)
        assertEquals(5, store.uiState.value.balanceCredits)
        assertEquals(5, hub.uiState.value.balanceCredits)
        session.value = "b:2"
        assertEquals(0, store.uiState.value.balanceCredits)
        assertEquals(0, hub.uiState.value.balanceCredits)
        session.value = "a:3"
        store.onEvent(CreditStoreUiEvent.PackageClicked("credits_100"))
        hub.onEvent(CreditHubUiEvent.BuyCreditsClicked)
        assertEquals(0, store.uiState.value.balanceCredits)
        assertEquals(0, hub.uiState.value.balanceCredits)
        assertEquals(false, store.uiState.value.packages.any { it.id == "credits_100" && it.isSelected })
    }
}
