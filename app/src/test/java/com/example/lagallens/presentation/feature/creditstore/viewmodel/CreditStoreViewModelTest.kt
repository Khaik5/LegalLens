package com.example.lagallens.presentation.feature.creditstore.viewmodel

import com.example.lagallens.data.datasource.local.datastore.CreditDemoLedger
import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiEffect
import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CreditStoreViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun topUpUsesSelectedPackageForEveryOption() = runTest {
        val model = CreditStoreViewModel(CreditDemoLedger(initialBalance = 5))
        listOf("credits_5", "credits_15", "credits_30", "credits_100").forEach { id ->
            model.onEvent(CreditStoreUiEvent.PackageClicked(id))
            val effect = async { model.uiEffect.first() }
            kotlinx.coroutines.yield()
            model.onEvent(CreditStoreUiEvent.TopUpClicked)
            assertEquals(CreditStoreUiEffect.NavigateToPurchaseConfirmation(id, 5), effect.await())
        }
    }
}
