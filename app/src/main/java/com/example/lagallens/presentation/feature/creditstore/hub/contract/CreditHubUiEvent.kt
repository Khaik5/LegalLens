package com.example.lagallens.presentation.feature.creditstore.hub.contract

sealed interface CreditHubUiEvent {
    data object BackClicked : CreditHubUiEvent
    data object BuyCreditsClicked : CreditHubUiEvent
    data object HistoryClicked : CreditHubUiEvent
    data object UnlockResultsClicked : CreditHubUiEvent
}
