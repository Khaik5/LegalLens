package com.example.lagallens.presentation.feature.creditstore.flow.contract

sealed interface CreditFlowUiEffect {
    data object NavigateBack : CreditFlowUiEffect
    data object NavigateToCreditStore : CreditFlowUiEffect
    data object NavigateToCreditHistory : CreditFlowUiEffect
    data object ReturnToAnalysisHistory : CreditFlowUiEffect
    data object ReturnToUnlockConfirmation : CreditFlowUiEffect
    data class ReturnUnlockedResult(val analysisResultId: String) : CreditFlowUiEffect
    data object PaymentRequested : CreditFlowUiEffect
}
