package com.example.lagallens.presentation.feature.contracts.detail.contract

import androidx.annotation.StringRes

sealed interface ContractDetailUiEffect {
    data object NavigateBack : ContractDetailUiEffect
    data class NavigateToDocument(val contractId: String) : ContractDetailUiEffect
    data class NavigateToCompare(val contractId: String) : ContractDetailUiEffect
    data class NavigateToOcrText(val documentId: String, val contractId: String, val documentName: String, @param:StringRes val documentDetailsRes: Int, val content: String) : ContractDetailUiEffect

    data class NavigateToAnalysis(
        @param:StringRes val documentNameRes: Int,
        @param:StringRes val documentMetadataRes: Int
    ) : ContractDetailUiEffect

    data class NavigateToChatbot(
        @param:StringRes val contractNameRes: Int,
        @param:StringRes val contractTypeRes: Int,
        val contractId: String
    ) : ContractDetailUiEffect
    data object ShowDeleteConfirmation : ContractDetailUiEffect
    data object ShowRenameDialog : ContractDetailUiEffect
    data object ShowExportSheet : ContractDetailUiEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : ContractDetailUiEffect
}
