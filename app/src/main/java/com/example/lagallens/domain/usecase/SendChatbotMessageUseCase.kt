package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.example.lagallens.domain.repository.ChatbotRepository

class SendChatbotMessageUseCase(
    private val repository: ChatbotRepository
) {
    suspend operator fun invoke(conversationId: String, message: String): ChatbotReply =
        repository.sendMessage(conversationId, message)
}
