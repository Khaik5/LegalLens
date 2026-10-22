package com.example.lagallens.presentation.feature.chatbot.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.data.repository.FakeChatbotRepository
import com.example.lagallens.domain.usecase.OpenChatbotConversationUseCase
import com.example.lagallens.domain.usecase.RetryChatbotMessageUseCase
import com.example.lagallens.domain.usecase.SendChatbotMessageUseCase
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotMessageAuthor
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatbotViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun screenOpenedLoadsTheConversationFromTheRepository() = runTest {
        val viewModel = viewModel()

        openScreen(viewModel)

        assertEquals("HĐ Thuê VP Bitexco", viewModel.uiState.value.contractName)
        assertEquals("Hợp đồng Thuê văn phòng", viewModel.uiState.value.contractType)
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals(4, viewModel.uiState.value.suggestions.size)
    }

    @Test
    fun sendClickedAppendsTheRepositoryReply() = runTest {
        val viewModel = viewModel()
        openScreen(viewModel)
        viewModel.onEvent(ChatbotUiEvent.MessageChanged("Điều khoản thanh toán thế nào?"))

        viewModel.onEvent(ChatbotUiEvent.SendClicked)

        assertEquals("", viewModel.uiState.value.messageDraft)
        assertEquals(3, viewModel.uiState.value.messages.size)
        assertEquals(ChatbotMessageAuthor.USER, viewModel.uiState.value.messages[1].author)
        assertEquals(
            "Tôi đã ghi nhận câu hỏi của bạn. Đây là phản hồi mẫu trong khi dữ liệu hội thoại theo hợp đồng chưa được kết nối với hệ thống.",
            viewModel.uiState.value.messages[2].content
        )
    }

    @Test
    fun citationQuestionCreatesCitedAnswer() = runTest {
        val viewModel = viewModel()
        openScreen(viewModel)

        viewModel.onEvent(ChatbotUiEvent.SuggestionClicked("Mức phạt vi phạm hợp đồng được quy định thế nào?"))

        assertEquals("Điều 301 · Luật Thương mại 2005", viewModel.uiState.value.messages[2].citation?.title)
    }

    @Test
    fun groundedQuestionCreatesAnswerWithContractReferences() = runTest {
        val viewModel = viewModel()
        openScreen(viewModel)

        viewModel.onEvent(ChatbotUiEvent.SuggestionClicked("Điều khoản chấm dứt hợp đồng gồm những gì?"))

        assertEquals("Tài liệu dẫn chiếu (2)", viewModel.uiState.value.messages[2].referenceSectionTitle)
        assertEquals(2, viewModel.uiState.value.messages[2].references.size)
    }

    @Test
    fun noEvidenceQuestionCreatesTheRepositoryWarning() = runTest {
        val viewModel = viewModel()
        openScreen(viewModel)

        viewModel.onEvent(ChatbotUiEvent.SuggestionClicked("Hợp đồng này có hỗ trợ chi phí đỗ xe ô tô không?"))

        assertEquals("Không đủ căn cứ trả lời", viewModel.uiState.value.messages[2].noEvidence?.title)
    }

    @Test
    fun retryReplacesTheRepositoryErrorWithFallbackAnswer() = runTest {
        val viewModel = viewModel()
        openScreen(viewModel)
        viewModel.onEvent(ChatbotUiEvent.SuggestionClicked("Tóm tắt nghĩa vụ của bên thuê"))

        viewModel.onEvent(ChatbotUiEvent.ErrorRetryClicked(viewModel.uiState.value.messages[2].id))

        assertEquals(null, viewModel.uiState.value.messages[2].error)
        assertEquals(
            "Tôi đã ghi nhận câu hỏi của bạn. Đây là phản hồi mẫu trong khi dữ liệu hội thoại theo hợp đồng chưa được kết nối với hệ thống.",
            viewModel.uiState.value.messages[2].content
        )
    }

    @Test
    fun newChatShowsTheRepositorySuggestions() = runTest {
        val viewModel = viewModel()
        openScreen(viewModel)
        viewModel.onEvent(ChatbotUiEvent.SuggestionClicked("Có điều khoản nào cần lưu ý?"))

        viewModel.onEvent(ChatbotUiEvent.NewChatClicked)

        assertEquals(0, viewModel.uiState.value.messages.size)
        assertEquals(3, viewModel.uiState.value.suggestions.size)
        assertEquals(true, viewModel.uiState.value.isNewConversation)
    }

    @Test
    fun drawerCanBeOpenedAndDismissed() = runTest {
        val viewModel = viewModel()
        openScreen(viewModel)

        viewModel.onEvent(ChatbotUiEvent.DrawerOpened)
        assertEquals(true, viewModel.uiState.value.isDrawerVisible)

        viewModel.onEvent(ChatbotUiEvent.DrawerDismissed)
        assertEquals(false, viewModel.uiState.value.isDrawerVisible)
    }

    @Test
    fun blankMessageDoesNotCreateARepositoryRequest() = runTest {
        val viewModel = viewModel()
        openScreen(viewModel)

        viewModel.onEvent(ChatbotUiEvent.SendClicked)

        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals(false, viewModel.uiState.value.isReplyPending)
    }

    private fun viewModel(): ChatbotViewModel {
        val repository = FakeChatbotRepository()
        return ChatbotViewModel(
            openConversation = OpenChatbotConversationUseCase(repository),
            sendMessage = SendChatbotMessageUseCase(repository),
            retryMessage = RetryChatbotMessageUseCase(repository)
        )
    }

    private fun openScreen(viewModel: ChatbotViewModel) {
        viewModel.onEvent(
            ChatbotUiEvent.ScreenOpened(
                contractName = "HĐ Thuê VP Bitexco",
                contractType = "Hợp đồng Thuê văn phòng"
            )
        )
    }
}
