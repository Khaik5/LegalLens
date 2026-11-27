package com.example.lagallens.presentation.feature.creditstore.unlock.contract

data class UnlockConfirmationUiState(
    val analysisResultId: String = "",
    val availableCredits: Int = 0,
    val unlockCost: Int = 0,
    val isConfirmationRequested: Boolean = false,
    val isInsufficientCreditsRequested: Boolean = false,
    val isBackendUnavailable: Boolean = false,
    /** After a successful unlock [availableCredits] already holds the balance left, so the cost is not taken again. */
    val isUnlocked: Boolean = false
) {
    val balanceBeforeUnlock: Int
        get() = if (isUnlocked) availableCredits + unlockCost else availableCredits

    val remainingCredits: Int
        get() = if (isUnlocked) availableCredits else (availableCredits - unlockCost).coerceAtLeast(0)
}
