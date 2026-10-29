package com.example.lagallens.presentation.feature.compare.select.contract

sealed interface CompareSelectUiEvent {
    data class Initialize(val initialId: String?) : CompareSelectUiEvent
    data class QueryChanged(val query: String) : CompareSelectUiEvent
    data class ContractClicked(val id: String) : CompareSelectUiEvent
    data object BackClicked : CompareSelectUiEvent
    data object CompareClicked : CompareSelectUiEvent
    data object ScreenResumed : CompareSelectUiEvent
}
