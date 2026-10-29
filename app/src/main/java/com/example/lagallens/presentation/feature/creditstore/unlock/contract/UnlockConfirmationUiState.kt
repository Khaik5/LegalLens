package com.example.lagallens.presentation.feature.creditstore.unlock.contract

data class UnlockConfirmationUiState(
    val analysisResultId: String = "",
    val availableCredits: Int = 0,
    val unlockCost: Int = 0,
    val isConfirmationRequested: Boolean = false,
    val isInsufficientCreditsRequested: Boolean = false,
    val isBackendUnavailable: Boolean = false
) {
    val remainingCredits: Int
        get() = (availableCredits - unlockCost).coerceAtLeast(0)
}
