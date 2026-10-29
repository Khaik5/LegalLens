package com.example.lagallens.presentation.feature.chatbot.help.contract

sealed interface ChatbotHelpUiEffect {
    data object Close : ChatbotHelpUiEffect
    data class ReturnQuestion(val question: String) : ChatbotHelpUiEffect
}
