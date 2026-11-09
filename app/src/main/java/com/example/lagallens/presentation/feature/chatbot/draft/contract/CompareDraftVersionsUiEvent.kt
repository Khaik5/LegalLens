package com.example.lagallens.presentation.feature.chatbot.draft.contract

sealed interface CompareDraftVersionsUiEvent {
    data class Opened(val contractId: String?) : CompareDraftVersionsUiEvent
}
