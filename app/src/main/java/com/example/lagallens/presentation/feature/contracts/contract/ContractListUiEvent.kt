package com.example.lagallens.presentation.feature.contracts.contract

sealed interface ContractListUiEvent {
    data class QueryChanged(val query: String) : ContractListUiEvent
    data class FilterSelected(val filter: ContractFilter) : ContractListUiEvent
    data object FilterClicked : ContractListUiEvent
    data class FiltersApplied(val filterSelection: ContractFilterSelection) : ContractListUiEvent
    data class ContractClicked(val contract: ContractListItem) : ContractListUiEvent
}
