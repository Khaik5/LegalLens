package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.chatbot.ChatbotConversation
import com.example.lagallens.domain.model.chatbot.ChatbotReply
import com.example.lagallens.domain.model.chatbot.ChatbotTranscript
import com.example.lagallens.domain.model.chatbot.ChatbotTranscriptMessage

interface ChatbotRepository {
    suspend fun openConversation(contractName: String, contractType: String, contractId: String? = null, conversationId: String? = null): ChatbotConversation

    suspend fun sendMessage(conversationId: String, message: String): ChatbotReply

    suspend fun retryMessage(conversationId: String, message: String): ChatbotReply

    suspend fun getConversations(): List<ChatbotTranscript> = emptyList()
    suspend fun getMessages(conversationId: String): List<ChatbotTranscriptMessage> = emptyList()
    suspend fun saveMessages(conversationId: String, contractId: String?, contractName: String, contractType: String, messages: List<ChatbotTranscriptMessage>) = Unit
    fun deleteConversation(conversationId: String) = Unit
    fun clearSessionData() = Unit
}
