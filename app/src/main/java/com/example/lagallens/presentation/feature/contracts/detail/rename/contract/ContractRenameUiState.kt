package com.example.lagallens.presentation.feature.contracts.detail.rename.contract

data class ContractRenameUiState(val name: String = "", val isInitialized: Boolean = false, val isTouched: Boolean = false, val isSaving: Boolean = false) {
    val hasError: Boolean get() = isTouched && name.isBlank()
    val canSave: Boolean get() = isInitialized && !isSaving && name.isNotBlank()
}
