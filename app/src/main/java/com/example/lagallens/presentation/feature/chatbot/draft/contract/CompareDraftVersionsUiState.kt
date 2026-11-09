package com.example.lagallens.presentation.feature.chatbot.draft.contract

data class CompareDraftVersionsUiState(
    val clause: String? = null,
    val before: String = "",
    val after: String = "",
    val isUnavailable: Boolean = false
)
