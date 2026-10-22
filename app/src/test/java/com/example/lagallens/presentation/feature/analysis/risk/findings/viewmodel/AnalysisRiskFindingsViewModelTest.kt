package com.example.lagallens.presentation.feature.analysis.risk.findings.viewmodel

import com.example.lagallens.presentation.feature.analysis.risk.findings.contract.AnalysisRiskFindingsUiEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalysisRiskFindingsViewModelTest {
    @Test
    fun screenOpenedPreservesDocumentContext() {
        val viewModel = AnalysisRiskFindingsViewModel()

        viewModel.onEvent(
            AnalysisRiskFindingsUiEvent.ScreenOpened(
                documentName = "Hợp đồng thuê văn phòng",
                documentMetadata = "Dịch vụ · 12 trang"
            )
        )

        assertEquals("Hợp đồng thuê văn phòng", viewModel.uiState.value.documentName)
        assertEquals("Dịch vụ · 12 trang", viewModel.uiState.value.documentMetadata)
    }
}
