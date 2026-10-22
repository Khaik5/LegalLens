package com.example.lagallens.presentation.feature.creditstore.unlock.contract

sealed interface UnlockConfirmationUiEvent {
    data class ScreenOpened(
        val analysisResultId: String,
        val availableCredits: Int,
        val unlockCost: Int
    ) : UnlockConfirmationUiEvent

    data object BackClicked : UnlockConfirmationUiEvent
    data object CancelClicked : UnlockConfirmationUiEvent
    data object ConfirmClicked : UnlockConfirmationUiEvent
}
