package com.example.lagallens.presentation.feature.contracts.detail.export.contract

sealed interface ContractExportUiEvent {
    data class Opened(val title: String, val metadata: String, val showReadyPreview: Boolean) : ContractExportUiEvent
    data object ExportClicked : ContractExportUiEvent
    data object ShareClicked : ContractExportUiEvent
    data object RetryClicked : ContractExportUiEvent
    data object CancelClicked : ContractExportUiEvent
}
