package com.example.lagallens.domain.usecase

import com.example.lagallens.domain.model.chatbot.ChatbotConversation
import com.example.lagallens.domain.repository.ChatbotRepository
import com.example.lagallens.domain.model.chatbot.ChatbotTranscript
import com.example.lagallens.domain.model.chatbot.ChatbotTranscriptMessage

class OpenChatbotConversationUseCase(
    private val repository: ChatbotRepository
) {
    fun currentSessionKey() = repository.currentSessionKey()
    fun observeSessionKeys() = repository.observeSessionKeys()
    suspend fun messagesForSession(session: String?, conversationId: String) = repository.messagesForSession(session, conversationId)
    suspend fun openForSession(session: String?, contractName: String, contractType: String, contractId: String?, conversationId: String? = null) =
        repository.openForSession(session, contractName, contractType, contractId, conversationId)
    suspend fun saveForSession(session: String?, conversationId: String, contractId: String?, contractName: String, contractType: String, messages: List<ChatbotTranscriptMessage>) =
        repository.saveForSession(session, conversationId, contractId, contractName, contractType, messages)
    suspend operator fun invoke(contractName: String, contractType: String, contractId: String? = null, conversationId: String? = null): ChatbotConversation =
        repository.openConversation(contractName, contractType, contractId, conversationId)

    suspend fun messages(conversationId: String): List<ChatbotTranscriptMessage> = repository.getMessages(conversationId)
    suspend fun conversations(): List<ChatbotTranscript> = repository.getConversations()
    suspend fun save(conversationId: String, contractId: String?, contractName: String, contractType: String, messages: List<ChatbotTranscriptMessage>) =
        repository.saveMessages(conversationId, contractId, contractName, contractType, messages)
}
