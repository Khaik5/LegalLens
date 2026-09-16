package com.example.lagallens.presentation.feature.contracts.detail.contract

sealed interface ContractDetailUiEvent {
    data object BackClicked : ContractDetailUiEvent
    data class AssistantActionClicked(val action: ContractAssistantAction) : ContractDetailUiEvent
    data object RenameClicked : ContractDetailUiEvent
    data object DeleteClicked : ContractDetailUiEvent
    data object DeleteConfirmed : ContractDetailUiEvent
}
