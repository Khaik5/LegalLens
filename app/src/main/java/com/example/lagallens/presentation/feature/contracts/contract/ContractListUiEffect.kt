package com.example.lagallens.presentation.feature.contracts.contract

import androidx.annotation.StringRes

sealed interface ContractListUiEffect {
    data class ShowMessage(@StringRes val messageRes: Int) : ContractListUiEffect
}
