package com.example.lagallens.presentation.feature.contracts.detail.export.contract

enum class ContractExportStatus { OPTIONS, LOADING, ERROR }

data class ContractExportUiState(
    val title: String = "",
    val metadata: String = "",
    val status: ContractExportStatus = ContractExportStatus.OPTIONS,
    val isInitialized: Boolean = false
)
