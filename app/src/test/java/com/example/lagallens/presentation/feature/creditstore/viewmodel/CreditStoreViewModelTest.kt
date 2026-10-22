package com.example.lagallens.presentation.feature.creditstore.viewmodel

import com.example.lagallens.presentation.feature.creditstore.contract.CreditStoreUiEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditStoreViewModelTest {
    @Test
    fun selectingPackageUpdatesTheSelectedPackageOnly() {
        val viewModel = CreditStoreViewModel()

        viewModel.onEvent(CreditStoreUiEvent.PackageClicked("credits_30"))

        val packages = viewModel.uiState.value.packages
        assertTrue(packages.single { it.id == "credits_30" }.isSelected)
        assertFalse(packages.single { it.id == "credits_15" }.isSelected)
        assertEquals(1, packages.count { it.isSelected })
    }

    @Test
    fun selectingUnknownPackageKeepsCurrentSelection() {
        val viewModel = CreditStoreViewModel()

        viewModel.onEvent(CreditStoreUiEvent.PackageClicked("unknown"))

        assertTrue(viewModel.uiState.value.packages.single { it.id == "credits_15" }.isSelected)
        assertEquals(1, viewModel.uiState.value.packages.count { it.isSelected })
    }
}
