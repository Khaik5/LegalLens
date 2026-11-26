package com.example.lagallens.presentation.feature.chatbot.draft.save.detail

sealed interface SavedDraftCopyDetailUiEvent {
    data class SaveAsContractClicked(val draftId: String) : SavedDraftCopyDetailUiEvent
    data object BackClicked : SavedDraftCopyDetailUiEvent
}
