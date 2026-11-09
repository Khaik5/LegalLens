package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.example.lagallens.domain.repository.ChatbotRepository

class RetryChatbotMessageUseCase(
    private val repository: ChatbotRepository
) {
    suspend fun forSession(session: String?, conversationId: String, message: String, attachment: com.example.lagallens.domain.model.chatbot.ChatbotAttachment? = null): ChatbotReply =
        repository.retryForSession(session, conversationId, message, attachment)
    suspend operator fun invoke(conversationId: String, message: String): ChatbotReply =
        repository.retryMessage(conversationId, message)
}
