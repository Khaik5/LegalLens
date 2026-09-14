package com.example.lagallens.presentation.feature.contracts.detail.contract

import androidx.annotation.StringRes

sealed interface ContractDetailUiEffect {
    data object NavigateBack : ContractDetailUiEffect
    data object ShowDeleteConfirmation : ContractDetailUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : ContractDetailUiEffect
}
