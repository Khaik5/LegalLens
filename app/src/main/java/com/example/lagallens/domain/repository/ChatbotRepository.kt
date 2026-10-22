package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.chatbot.ChatbotConversation
import com.example.lagallens.domain.model.chatbot.ChatbotReply

interface ChatbotRepository {
    suspend fun openConversation(contractName: String, contractType: String): ChatbotConversation

    suspend fun sendMessage(conversationId: String, message: String): ChatbotReply

    suspend fun retryMessage(conversationId: String, message: String): ChatbotReply
}
