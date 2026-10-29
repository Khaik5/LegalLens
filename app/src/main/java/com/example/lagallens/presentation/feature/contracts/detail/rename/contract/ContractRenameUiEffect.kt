package com.example.lagallens.presentation.feature.contracts.detail.rename.contract

sealed interface ContractRenameUiEffect {
    data object Close : ContractRenameUiEffect
    data class Saved(val name: String) : ContractRenameUiEffect
}
