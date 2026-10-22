package com.example.lagallens.presentation.feature.creditstore.flow.contract

sealed interface CreditFlowUiEvent {
    data object BackClicked : CreditFlowUiEvent
    data object ContinueFreeClicked : CreditFlowUiEvent
    data object OpenCreditStoreClicked : CreditFlowUiEvent
    data object OpenCreditHistoryClicked : CreditFlowUiEvent
    data object OpenAnalysisHistoryClicked : CreditFlowUiEvent
    data object RequestPaymentClicked : CreditFlowUiEvent
    data object ReturnToUnlockClicked : CreditFlowUiEvent
    data class OpenUnlockedResultClicked(val analysisResultId: String) : CreditFlowUiEvent
}
