package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.chatbot.ChatbotConversation
import com.example.lagallens.domain.repository.ChatbotRepository

class OpenChatbotConversationUseCase(
    private val repository: ChatbotRepository
) {
    suspend operator fun invoke(contractName: String, contractType: String): ChatbotConversation =
        repository.openConversation(contractName, contractType)
}
