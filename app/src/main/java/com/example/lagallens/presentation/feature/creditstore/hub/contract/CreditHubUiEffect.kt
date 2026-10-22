package com.example.lagallens.presentation.feature.creditstore.hub.contract

sealed interface CreditHubUiEffect {
    data object NavigateBack : CreditHubUiEffect
    data object NavigateToStore : CreditHubUiEffect
    data object NavigateToHistory : CreditHubUiEffect
    data object NavigateToUnlockConfirmation : CreditHubUiEffect
}
