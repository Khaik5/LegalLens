package com.example.lagallens.presentation.feature.contracts.contract

import androidx.annotation.StringRes

sealed interface ContractListUiEffect {
    data object ShowFilterBottomSheet : ContractListUiEffect
    data class NavigateToDetail(val contractId: String) : ContractListUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : ContractListUiEffect
}
