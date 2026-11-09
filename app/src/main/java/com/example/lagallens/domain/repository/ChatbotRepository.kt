package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.chatbot.ChatbotAttachment
import com.example.lagallens.domain.model.chatbot.ChatbotConversation
import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.example.lagallens.domain.model.chatbot.ChatbotTranscript
import com.example.lagallens.domain.model.chatbot.ChatbotTranscriptMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ChatbotRepository {
    fun currentSessionKey(): String? = null
    fun observeSessionKeys(): Flow<String?> = flowOf(currentSessionKey())
    suspend fun conversationsForSession(sessionKey: String?): List<ChatbotTranscript> {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        return getConversations()
    }
    suspend fun messagesForSession(sessionKey: String?, conversationId: String): List<ChatbotTranscriptMessage> {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        return getMessages(conversationId)
    }
    suspend fun sendForSession(sessionKey: String?, conversationId: String, message: String, attachment: ChatbotAttachment? = null): ChatbotReply {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        return sendMessage(conversationId, message)
    }
    suspend fun retryForSession(sessionKey: String?, conversationId: String, message: String, attachment: ChatbotAttachment? = null): ChatbotReply {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        return retryMessage(conversationId, message)
    }
    suspend fun openForSession(sessionKey: String?, contractName: String, contractType: String, contractId: String?, conversationId: String?): ChatbotConversation {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        return openConversation(contractName, contractType, contractId, conversationId)
    }
    suspend fun saveForSession(sessionKey: String?, conversationId: String, contractId: String?, contractName: String, contractType: String, messages: List<ChatbotTranscriptMessage>) {
        check(currentSessionKey() == sessionKey) { "Session changed" }
        saveMessages(conversationId, contractId, contractName, contractType, messages)
    }
    suspend fun openConversation(contractName: String, contractType: String, contractId: String? = null, conversationId: String? = null): ChatbotConversation

    suspend fun sendMessage(conversationId: String, message: String): ChatbotReply

    suspend fun retryMessage(conversationId: String, message: String): ChatbotReply

    suspend fun getConversations(): List<ChatbotTranscript> = emptyList()
    suspend fun getMessages(conversationId: String): List<ChatbotTranscriptMessage> = emptyList()
    suspend fun saveMessages(conversationId: String, contractId: String?, contractName: String, contractType: String, messages: List<ChatbotTranscriptMessage>) = Unit
    fun deleteConversation(conversationId: String) = Unit
    suspend fun deleteConversationsForContract(contractId: String) = Unit
    suspend fun renameContract(contractId: String, title: String) = Unit
    fun clearSessionData() = Unit
}
