package com.example.lagallens.presentation.feature.chatbot.viewmodel

import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotMessageAuthor
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatbotViewModelTest {
    @Test
    fun screenOpenedRendersProvidedContractAndConversation() {
        val viewModel = ChatbotViewModel()

        viewModel.onEvent(screenOpenedEvent())

        assertEquals("HĐ Thuê VP Bitexco", viewModel.uiState.value.contractName)
        assertEquals("Hợp đồng Thuê văn phòng", viewModel.uiState.value.contractType)
        assertEquals(3, viewModel.uiState.value.messages.size)
    }

    @Test
    fun sendClickedAppendsUserQuestionAndLocalResponse() {
        val viewModel = ChatbotViewModel()
        viewModel.onEvent(screenOpenedEvent())
        viewModel.onEvent(ChatbotUiEvent.MessageChanged("Điều khoản thanh toán thế nào?"))

        viewModel.onEvent(ChatbotUiEvent.SendClicked)

        assertEquals("", viewModel.uiState.value.messageDraft)
        assertEquals(5, viewModel.uiState.value.messages.size)
        assertEquals(ChatbotMessageAuthor.USER, viewModel.uiState.value.messages[3].author)
        assertEquals("Phản hồi mẫu", viewModel.uiState.value.messages[4].content)
    }

    private fun screenOpenedEvent() = ChatbotUiEvent.ScreenOpened(
        contractName = "HĐ Thuê VP Bitexco",
        contractType = "Hợp đồng Thuê văn phòng",
        initialGreeting = "Lời chào",
        initialQuestion = "Câu hỏi",
        initialAnswer = "Câu trả lời",
        localResponse = "Phản hồi mẫu"
    )
}
