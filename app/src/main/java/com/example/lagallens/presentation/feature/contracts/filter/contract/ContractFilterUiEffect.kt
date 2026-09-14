package com.example.lagallens.presentation.feature.contracts.filter.contract

import com.example.lagallens.presentation.feature.contracts.contract.ContractFilterSelection

sealed interface ContractFilterUiEffect {
    data class ApplyFilters(val filterSelection: ContractFilterSelection) : ContractFilterUiEffect
}
