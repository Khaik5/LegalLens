package com.example.lagallens.presentation.feature.contracts.detail.export.contract

sealed interface ContractExportUiEffect {
    data object Close : ContractExportUiEffect

    /** [fileUri] is a downloaded copy of the contract's original document. */
    data class SaveFile(val fileUri: String, val fileName: String, val mimeType: String) : ContractExportUiEffect
    data class ShareFile(val fileUri: String, val fileName: String, val mimeType: String) : ContractExportUiEffect
}
