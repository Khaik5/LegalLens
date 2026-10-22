package com.example.lagallens.presentation.feature.contracts.detail.contract

import androidx.annotation.StringRes

sealed interface ContractDetailUiEffect {
    data object NavigateBack : ContractDetailUiEffect
    data class NavigateToOcrText(
        @param:StringRes val documentNameRes: Int,
        @param:StringRes val documentDetailsRes: Int
    ) : ContractDetailUiEffect

    data class NavigateToAnalysis(
        @param:StringRes val documentNameRes: Int,
        @param:StringRes val documentMetadataRes: Int
    ) : ContractDetailUiEffect

    data class NavigateToChatbot(
        @param:StringRes val contractNameRes: Int,
        @param:StringRes val contractTypeRes: Int
    ) : ContractDetailUiEffect
    data object ShowDeleteConfirmation : ContractDetailUiEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : ContractDetailUiEffect
}
