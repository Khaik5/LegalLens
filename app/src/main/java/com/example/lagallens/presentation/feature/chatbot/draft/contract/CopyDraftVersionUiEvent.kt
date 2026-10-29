package com.example.lagallens.presentation.feature.chatbot.draft.contract

sealed interface CopyDraftVersionUiEvent {
    data class Opened(val previewFailure: Boolean, val sourceDraftId: String?, val contractId: String?) : CopyDraftVersionUiEvent
    data object RetryClicked : CopyDraftVersionUiEvent
}
