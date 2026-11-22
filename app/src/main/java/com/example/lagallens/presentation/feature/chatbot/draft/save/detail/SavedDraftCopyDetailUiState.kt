package com.example.lagallens.presentation.feature.chatbot.draft.save.detail

/** [UNAVAILABLE]: this draft can never become a contract, so the action is off instead of inviting retries. */
enum class SaveAsContractStatus { IDLE, SAVING, SAVED, UNAVAILABLE }

data class SavedDraftCopyDetailUiState(
    val saveAsContractStatus: SaveAsContractStatus = SaveAsContractStatus.IDLE
)
