package com.example.lagallens.presentation.feature.chatbot.draft.contract

data class ChatbotDraftManualEditUiState(
    val body: String = "",
    val addition: String = "",
    val isDirty: Boolean = false,
    val isSaving: Boolean = false
)
