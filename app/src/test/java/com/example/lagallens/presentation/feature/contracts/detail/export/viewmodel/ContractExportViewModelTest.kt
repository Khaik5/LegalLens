package com.example.lagallens.presentation.feature.contracts.detail.export.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportStatus
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportUiEvent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ContractExportViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    @Test fun unavailableFileNeverProducesReadyOnSaveShareOrRetry() {
        listOf(ContractExportUiEvent.ExportClicked, ContractExportUiEvent.ShareClicked).forEach { action ->
            val vm = ContractExportViewModel()
            vm.onEvent(ContractExportUiEvent.Opened("document", "metadata", false))
            vm.onEvent(action)
            assertEquals(ContractExportStatus.ERROR, vm.uiState.value.status)
            vm.onEvent(ContractExportUiEvent.RetryClicked)
            assertEquals(ContractExportStatus.ERROR, vm.uiState.value.status)
        }
    }

    @Test fun previewFlagCannotClaimFileIsReady() {
        val vm = ContractExportViewModel()
        vm.onEvent(ContractExportUiEvent.Opened("document", "metadata", true))
        assertEquals(ContractExportStatus.ERROR, vm.uiState.value.status)
    }
}
