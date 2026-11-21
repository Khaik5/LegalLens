package com.example.lagallens.presentation.feature.chatbot.draft.save.detail

enum class SaveAsContractStatus { IDLE, SAVING, SAVED }

data class SavedDraftCopyDetailUiState(
    val saveAsContractStatus: SaveAsContractStatus = SaveAsContractStatus.IDLE
)
