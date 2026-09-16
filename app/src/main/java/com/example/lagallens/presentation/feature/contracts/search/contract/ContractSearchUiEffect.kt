package com.example.lagallens.presentation.feature.contracts.search.contract

import androidx.annotation.StringRes

sealed interface ContractSearchUiEffect {
    data object NavigateBack : ContractSearchUiEffect
    data class NavigateToDetail(val contractId: String) : ContractSearchUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : ContractSearchUiEffect
}
