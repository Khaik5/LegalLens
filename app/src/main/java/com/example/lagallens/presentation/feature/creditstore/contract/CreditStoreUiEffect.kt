package com.example.lagallens.presentation.feature.creditstore.contract

sealed interface CreditStoreUiEffect {
    data object ShowBackendUnavailable : CreditStoreUiEffect
    data object NavigateBack : CreditStoreUiEffect
    data class NavigateToPurchaseConfirmation(val packageId: String, val balanceCredits: Int) : CreditStoreUiEffect
    data object NavigateToCreditHistory : CreditStoreUiEffect
}
