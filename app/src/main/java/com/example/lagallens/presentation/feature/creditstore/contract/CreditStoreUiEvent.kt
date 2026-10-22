package com.example.lagallens.presentation.feature.creditstore.contract

sealed interface CreditStoreUiEvent {
    data object BackClicked : CreditStoreUiEvent
    data object TopUpClicked : CreditStoreUiEvent
    data object ViewHistoryClicked : CreditStoreUiEvent
    data class PackageClicked(val packageId: String) : CreditStoreUiEvent
}
