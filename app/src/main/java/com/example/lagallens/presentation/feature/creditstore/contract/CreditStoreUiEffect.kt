package com.example.lagallens.presentation.feature.creditstore.contract

sealed interface CreditStoreUiEffect {
    data object ShowBackendUnavailable : CreditStoreUiEffect
    data object NavigateBack : CreditStoreUiEffect
    data class NavigateToPurchaseConfirmation(val creditPackage: CreditPackage, val balanceCredits: Int) : CreditStoreUiEffect
    data object NavigateToCreditHistory : CreditStoreUiEffect
}
