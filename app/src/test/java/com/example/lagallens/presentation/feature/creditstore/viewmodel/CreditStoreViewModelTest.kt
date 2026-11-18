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

    @Test fun packageNetworkFailureDoesNotPretendBackendIsUnconfigured() = runTest {
        val payments = object : com.example.lagallens.TestPaymentRepository() {
            override suspend fun getPackages(): List<com.example.lagallens.domain.model.credit.CreditOffer> = throw java.io.IOException("offline")
        }
        val model = CreditStoreViewModel(CreditDemoLedger(initialBalance = 5), payments)
        assertEquals(false, model.uiState.value.isLoading)
        assertEquals(false, model.uiState.value.isBackendUnavailable)
    }

    @Test fun topUpWithNoPackagesProducesFeedback() = runTest {
        val payments = object : com.example.lagallens.TestPaymentRepository() {
            override suspend fun getPackages() = emptyList<com.example.lagallens.domain.model.credit.CreditOffer>()
        }
        val model = CreditStoreViewModel(CreditDemoLedger(initialBalance = 5), payments)
        model.onEvent(CreditStoreUiEvent.TopUpClicked)
        val effect = kotlinx.coroutines.withTimeoutOrNull(100) { model.uiEffect.first() }
        org.junit.Assert.assertNotNull(effect)
    }

    @Test fun retryRecoversBothStoreAndHubFromTransientFailures() = runTest {
        var offline = true
        var walletLoads = 0
        val credit = object : CreditRepository by CreditDemoLedger(initialBalance = 5) {
            override suspend fun refresh() { walletLoads++; if (offline) throw java.io.IOException("offline") }
        }
        val payments = object : com.example.lagallens.TestPaymentRepository() {
            override suspend fun getPackages(): List<com.example.lagallens.domain.model.credit.CreditOffer> {
                if (offline) throw java.io.IOException("offline")
                return offers
            }
        }
        val store = CreditStoreViewModel(credit, payments)
        val hub = CreditHubViewModel(credit)
        assertEquals(true, store.uiState.value.isPackageLoadFailed)
        assertEquals(true, store.uiState.value.isBalanceUnavailable)
        assertEquals(true, hub.uiState.value.isBalanceUnavailable)
        assertEquals(false, hub.uiState.value.isBackendUnavailable)
        offline = false
        store.onEvent(CreditStoreUiEvent.RetryClicked)
        hub.onEvent(CreditHubUiEvent.RetryClicked)
        assertEquals(4, walletLoads)
        assertEquals(false, store.uiState.value.isPackageLoadFailed)
        assertEquals(false, store.uiState.value.isBalanceUnavailable)
        assertEquals(false, hub.uiState.value.isBalanceUnavailable)
        assertEquals(4, store.uiState.value.packages.size)
    }

    @Test fun missingConfigurationIsDistinctAndDoesNotAttemptNetworkLoads() = runTest {
        val credit = object : CreditRepository by CreditDemoLedger(initialBalance = 0) {
            override val isConfigured = false
            override suspend fun refresh(): Unit = error("Unconfigured wallet must not load")
        }
        val payments = object : com.example.lagallens.TestPaymentRepository() {
            override val isConfigured = false
            override suspend fun getPackages(): List<com.example.lagallens.domain.model.credit.CreditOffer> = error("Unconfigured payments must not load")
        }
        val store = CreditStoreViewModel(credit, payments)
        val hub = CreditHubViewModel(credit)
        assertEquals(true, store.uiState.value.isBackendUnavailable)
        assertEquals(true, store.uiState.value.isPaymentUnavailable)
        assertEquals(false, store.uiState.value.isPackageLoadFailed)
        assertEquals(false, store.uiState.value.isBalanceUnavailable)
        assertEquals(true, hub.uiState.value.isBackendUnavailable)
        assertEquals(false, hub.uiState.value.isBalanceUnavailable)
    }

    @Test fun firstOfferIsSelectedWhenNoneIsRecommended() = runTest {
        val payments = object : com.example.lagallens.TestPaymentRepository() {
            override suspend fun getPackages() = offers.map { it.copy(isRecommended = false) }
        }
        val model = CreditStoreViewModel(CreditDemoLedger(initialBalance = 5), payments)
        assertEquals(1, model.uiState.value.packages.count { it.isSelected })
        model.onEvent(CreditStoreUiEvent.TopUpClicked)
        assertEquals("credits_5", (model.uiEffect.first() as CreditStoreUiEffect.NavigateToPurchaseConfirmation).creditPackage.id)
    }

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
