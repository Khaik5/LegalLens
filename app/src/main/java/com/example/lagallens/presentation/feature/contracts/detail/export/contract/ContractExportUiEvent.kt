package com.example.lagallens.presentation.feature.contracts.detail.export.contract

sealed interface ContractExportUiEvent {
    data class Opened(val title: String, val metadata: String) : ContractExportUiEvent
    data object ExportClicked : ContractExportUiEvent
    data object ShareClicked : ContractExportUiEvent
    data object RetryClicked : ContractExportUiEvent
    data object CancelClicked : ContractExportUiEvent
    /** The screen no longer needs a downloaded copy it did not hand to another app. */
    data class DownloadReleased(val fileUri: String) : ContractExportUiEvent
}
