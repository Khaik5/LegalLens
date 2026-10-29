package com.example.lagallens.presentation.feature.chatbot.help.contract

sealed interface ChatbotHelpUiEvent {
    data object BackClicked : ChatbotHelpUiEvent
    data class FaqClicked(val index: Int) : ChatbotHelpUiEvent
    data class QuestionClicked(val question: String) : ChatbotHelpUiEvent
}
