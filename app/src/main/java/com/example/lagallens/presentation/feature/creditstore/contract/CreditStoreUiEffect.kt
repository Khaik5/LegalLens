package com.example.lagallens.presentation.feature.creditstore.contract

sealed interface CreditStoreUiEffect {
    data object NavigateBack : CreditStoreUiEffect
    data object NavigateToPurchaseConfirmation : CreditStoreUiEffect
    data object NavigateToCreditHistory : CreditStoreUiEffect
}
