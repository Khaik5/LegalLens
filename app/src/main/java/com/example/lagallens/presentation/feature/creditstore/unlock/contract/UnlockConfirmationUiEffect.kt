package com.example.lagallens.presentation.feature.creditstore.unlock.contract

sealed interface UnlockConfirmationUiEffect {
    data object NavigateBack : UnlockConfirmationUiEffect
    data object NavigateToInsufficientCredits : UnlockConfirmationUiEffect
    data class UnlockRequested(
        val analysisResultId: String,
        val creditCost: Int
    ) : UnlockConfirmationUiEffect
}
