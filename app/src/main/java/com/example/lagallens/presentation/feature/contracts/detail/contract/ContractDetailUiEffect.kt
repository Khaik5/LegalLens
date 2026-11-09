package com.example.lagallens.presentation.feature.contracts.detail.contract

import androidx.annotation.StringRes

sealed interface ContractDetailUiEffect {
    data object NavigateBack : ContractDetailUiEffect
    data class NavigateToDocument(val contractId: String, val documentUri: String?, val title: String) : ContractDetailUiEffect
    data class NavigateToCompare(val contractId: String) : ContractDetailUiEffect
    data class NavigateToOcrText(val documentId: String, val contractId: String, val documentName: String, val documentDetails: String, val content: String, val ownerSession: String?) : ContractDetailUiEffect

    data class NavigateToAnalysis(
        val documentName: String,
        val documentMetadata: String,
        val contractId: String = "",
        val documentId: String = ""
    ) : ContractDetailUiEffect

    data class NavigateToChatbot(
        val contractName: String,
        @param:StringRes val contractTypeRes: Int,
        val contractId: String
    ) : ContractDetailUiEffect
    data object ShowDeleteConfirmation : ContractDetailUiEffect
    data object ShowRenameDialog : ContractDetailUiEffect
    data object ShowExportSheet : ContractDetailUiEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : ContractDetailUiEffect
}
