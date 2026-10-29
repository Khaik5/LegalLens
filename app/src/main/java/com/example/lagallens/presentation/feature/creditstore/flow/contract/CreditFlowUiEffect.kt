package com.example.lagallens.presentation.feature.creditstore.flow.contract

import com.example.lagallens.domain.model.payment.PaymentRecord

sealed interface CreditFlowUiEffect {
    data object NavigateBack : CreditFlowUiEffect
    data object NavigateToCreditStore : CreditFlowUiEffect
    data object NavigateToCreditHistory : CreditFlowUiEffect
    data object ReturnToAnalysisHistory : CreditFlowUiEffect
    data object ReturnToUnlockConfirmation : CreditFlowUiEffect
    data class ReturnUnlockedResult(val analysisResultId: String) : CreditFlowUiEffect
    data object PaymentRequested : CreditFlowUiEffect
    data class PaymentSucceeded(val payment: PaymentRecord) : CreditFlowUiEffect
    data object PaymentFailed : CreditFlowUiEffect
}
