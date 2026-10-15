package com.example.lagallens.presentation.feature.chatbot.viewmodel

import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotMessageAuthor
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotCitation
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotError
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotNoEvidence
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotReference
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatbotViewModelTest {
    @Test
    fun screenOpenedRendersProvidedContractGreetingAndSuggestions() {
        val viewModel = ChatbotViewModel()

        viewModel.onEvent(screenOpenedEvent())

        assertEquals("HĐ Thuê VP Bitexco", viewModel.uiState.value.contractName)
        assertEquals("Hợp đồng Thuê văn phòng", viewModel.uiState.value.contractType)
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals(listOf("Câu hỏi gợi ý"), viewModel.uiState.value.suggestions)
    }

    @Test
    fun sendClickedAppendsUserQuestionAndLocalResponse() {
        val viewModel = ChatbotViewModel()
        viewModel.onEvent(screenOpenedEvent())
        viewModel.onEvent(ChatbotUiEvent.MessageChanged("Điều khoản thanh toán thế nào?"))

        viewModel.onEvent(ChatbotUiEvent.SendClicked)

        assertEquals("", viewModel.uiState.value.messageDraft)
        assertEquals(3, viewModel.uiState.value.messages.size)
        assertEquals(ChatbotMessageAuthor.USER, viewModel.uiState.value.messages[1].author)
        assertEquals("Phản hồi mẫu", viewModel.uiState.value.messages[2].content)
        assertEquals(emptyList<String>(), viewModel.uiState.value.suggestions)
    }

    @Test
    fun suggestionClickedStartsConversationAndHidesSuggestions() {
        val viewModel = ChatbotViewModel()
        viewModel.onEvent(screenOpenedEvent())

        viewModel.onEvent(ChatbotUiEvent.SuggestionClicked("Câu hỏi gợi ý"))

        assertEquals(3, viewModel.uiState.value.messages.size)
        assertEquals("Câu hỏi gợi ý", viewModel.uiState.value.messages[1].content)
        assertEquals(emptyList<String>(), viewModel.uiState.value.suggestions)
    }

    @Test
    fun citationQuestionCreatesCitedAnswer() {
        val viewModel = ChatbotViewModel()
        viewModel.onEvent(screenOpenedEvent())

        viewModel.onEvent(ChatbotUiEvent.MessageChanged("Câu hỏi trích dẫn"))
        viewModel.onEvent(ChatbotUiEvent.SendClicked)

        assertEquals("Câu trả lời trích dẫn", viewModel.uiState.value.messages[2].content)
        assertEquals("Tiêu đề trích dẫn", viewModel.uiState.value.messages[2].citation?.title)
    }

    @Test
    fun groundedQuestionCreatesAnswerWithContractReferences() {
        val viewModel = ChatbotViewModel()
        viewModel.onEvent(screenOpenedEvent())

        viewModel.onEvent(ChatbotUiEvent.MessageChanged("Câu hỏi grounded"))
        viewModel.onEvent(ChatbotUiEvent.SendClicked)

        assertEquals("Câu trả lời grounded", viewModel.uiState.value.messages[2].content)
        assertEquals("Tài liệu dẫn chiếu (2)", viewModel.uiState.value.messages[2].referenceSectionTitle)
        assertEquals(2, viewModel.uiState.value.messages[2].references.size)
    }

    @Test
    fun drawerCanBeOpenedAndDismissed() {
        val viewModel = ChatbotViewModel()
        viewModel.onEvent(screenOpenedEvent())

        viewModel.onEvent(ChatbotUiEvent.DrawerOpened)
        assertEquals(true, viewModel.uiState.value.isDrawerVisible)

        viewModel.onEvent(ChatbotUiEvent.DrawerDismissed)
        assertEquals(false, viewModel.uiState.value.isDrawerVisible)
    }

    @Test
    fun newChatShowsTheNewConversationState() {
        val viewModel = ChatbotViewModel()
        viewModel.onEvent(screenOpenedEvent())
        viewModel.onEvent(ChatbotUiEvent.SuggestionClicked("Câu hỏi gợi ý"))

        viewModel.onEvent(ChatbotUiEvent.NewChatClicked)

        assertEquals(0, viewModel.uiState.value.messages.size)
        assertEquals(listOf("Câu hỏi trò chuyện mới"), viewModel.uiState.value.suggestions)
        assertEquals(true, viewModel.uiState.value.isNewConversation)
        assertEquals(false, viewModel.uiState.value.isDrawerVisible)
    }

    @Test
    fun noEvidenceQuestionCreatesWarningAnswer() {
        val viewModel = ChatbotViewModel()
        viewModel.onEvent(screenOpenedEvent())

        viewModel.onEvent(ChatbotUiEvent.MessageChanged("Câu hỏi không đủ căn cứ"))
        viewModel.onEvent(ChatbotUiEvent.SendClicked)

        assertEquals("Không đủ căn cứ", viewModel.uiState.value.messages[2].noEvidence?.title)
        assertEquals("Phản hồi không đủ căn cứ", viewModel.uiState.value.messages[2].content)
    }

    @Test
    fun errorQuestionCreatesRetryableErrorAnswer() {
        val viewModel = ChatbotViewModel()
        viewModel.onEvent(screenOpenedEvent())

        viewModel.onEvent(ChatbotUiEvent.MessageChanged("Câu hỏi lỗi"))
        viewModel.onEvent(ChatbotUiEvent.SendClicked)

        assertEquals("Không thể kết nối. Vui lòng thử lại.", viewModel.uiState.value.messages[2].error?.title)
    }

    @Test
    fun retryReplacesTheErrorWithLocalResponse() {
        val viewModel = ChatbotViewModel()
        viewModel.onEvent(screenOpenedEvent())
        viewModel.onEvent(ChatbotUiEvent.SuggestionClicked("Câu hỏi lỗi"))

        viewModel.onEvent(ChatbotUiEvent.ErrorRetryClicked(viewModel.uiState.value.messages[2].id))

        assertEquals("Phản hồi mẫu", viewModel.uiState.value.messages[2].content)
        assertEquals(null, viewModel.uiState.value.messages[2].error)
    }

    private fun screenOpenedEvent() = ChatbotUiEvent.ScreenOpened(
        contractName = "HĐ Thuê VP Bitexco",
        contractType = "Hợp đồng Thuê văn phòng",
        initialGreeting = "Lời chào",
        initialSuggestions = listOf("Câu hỏi gợi ý"),
        newChatSuggestions = listOf("Câu hỏi trò chuyện mới"),
        localResponse = "Phản hồi mẫu",
        citationQuestion = "Câu hỏi trích dẫn",
        citationResponse = "Câu trả lời trích dẫn",
        citation = ChatbotCitation(
            title = "Tiêu đề trích dẫn",
            description = "Mô tả trích dẫn",
            actionLabel = "Xem chi tiết"
        ),
        groundedQuestion = "Câu hỏi grounded",
        groundedResponse = "Câu trả lời grounded",
        groundedReferenceSectionTitle = "Tài liệu dẫn chiếu (2)",
        groundedReferences = listOf(
            ChatbotReference("Nguồn 1", "Trích đoạn 1"),
            ChatbotReference("Nguồn 2", "Trích đoạn 2")
        ),
        noEvidenceQuestion = "Câu hỏi không đủ căn cứ",
        noEvidence = ChatbotNoEvidence(
            title = "Không đủ căn cứ",
            message = "Phản hồi không đủ căn cứ"
        ),
        errorQuestion = "Câu hỏi lỗi",
        error = ChatbotError(
            title = "Không thể kết nối. Vui lòng thử lại.",
            retryLabel = "Thử lại"
        )
    )
}
