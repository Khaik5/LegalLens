package com.example.lagallens.presentation.feature.contracts.detail.contract

sealed interface ContractDetailUiEvent {
    data object BackClicked : ContractDetailUiEvent
    data class AssistantActionClicked(val action: ContractAssistantAction) : ContractDetailUiEvent
    data object RenameClicked : ContractDetailUiEvent
    data class RenameConfirmed(val name: String) : ContractDetailUiEvent
    data object DeleteClicked : ContractDetailUiEvent
    /** [confirmPaidResults] is true once the user accepted losing results unlocked with Credit. */
    data class DeleteConfirmed(val confirmPaidResults: Boolean = false) : ContractDetailUiEvent
}
