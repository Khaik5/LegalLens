package com.example.lagallens.presentation.feature.contracts.search.contract

sealed interface ContractSearchUiEvent {
    data class QueryChanged(val query: String) : ContractSearchUiEvent
    data class RecentSearchClicked(val query: String) : ContractSearchUiEvent
    data object ClearClicked : ContractSearchUiEvent
    data object BackClicked : ContractSearchUiEvent
    data class ResultClicked(val result: ContractSearchResult) : ContractSearchUiEvent
}
