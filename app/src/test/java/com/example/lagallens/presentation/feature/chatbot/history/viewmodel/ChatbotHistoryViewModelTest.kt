package com.example.lagallens.presentation.feature.chatbot.history.viewmodel

import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryConversation
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatbotHistoryViewModelTest {
    @Test
    fun screenOpenedRendersProvidedConversations() {
        val viewModel = ChatbotHistoryViewModel()
        val conversations = listOf(
            ChatbotHistoryConversation(
                id = 1L,
                contractName = "HĐ Thuê VP Bitexco",
                timestamp = "Hôm nay · 09:41",
                messageCount = "8 tin nhắn",
                preview = "Mức phạt vi phạm thỏa thuận là 15%..."
            )
        )

        viewModel.onEvent(ChatbotHistoryUiEvent.ScreenOpened(conversations))

        assertEquals(conversations, viewModel.uiState.value.conversations)
    }
}
