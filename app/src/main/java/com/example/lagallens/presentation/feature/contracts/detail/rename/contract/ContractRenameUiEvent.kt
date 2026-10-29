package com.example.lagallens.presentation.feature.contracts.detail.rename.contract

sealed interface ContractRenameUiEvent {
    data class Opened(val name: String) : ContractRenameUiEvent
    data class NameChanged(val name: String) : ContractRenameUiEvent
    data object SaveClicked : ContractRenameUiEvent
    data object CancelClicked : ContractRenameUiEvent
}
