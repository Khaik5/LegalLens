package com.example.lagallens.presentation.feature.chatbot.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.data.repository.FakeChatbotRepository
import com.example.lagallens.domain.usecase.OpenChatbotConversationUseCase
import com.example.lagallens.domain.usecase.RetryChatbotMessageUseCase
import com.example.lagallens.domain.usecase.SendChatbotMessageUseCase
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotMessageAuthor
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEvent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
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

        assertEquals("HĐ Lao động mẫu", viewModel.uiState.value.contractName)
        assertEquals("Hợp đồng lao động", viewModel.uiState.value.contractType)
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
            "[DEMO] Tôi đã ghi nhận câu hỏi về hợp đồng lao động. Phản hồi này chỉ minh họa giao diện, chưa được phân tích bởi AI và không phải tư vấn pháp lý.",
            viewModel.uiState.value.messages[2].content
        )
    }

    @Test
    fun citationQuestionCreatesCitedAnswer() = runTest {
        val viewModel = viewModel()
        openScreen(viewModel)

        viewModel.onEvent(ChatbotUiEvent.SuggestionClicked("Lương trong hợp đồng được ghi thế nào?"))

        assertEquals(true, viewModel.uiState.value.messages[2].content.contains("[DEMO]"))
    }

    @Test
    fun groundedQuestionCreatesAnswerWithContractReferences() = runTest {
        val viewModel = viewModel()
        openScreen(viewModel)

        viewModel.onEvent(ChatbotUiEvent.SuggestionClicked("Giải thích thử việc"))

        assertEquals(true, viewModel.uiState.value.messages[2].content.contains("hợp đồng lao động"))
    }

    @Test
    fun noEvidenceQuestionCreatesTheRepositoryWarning() = runTest {
        val viewModel = viewModel()
        openScreen(viewModel)

        viewModel.onEvent(ChatbotUiEvent.SuggestionClicked("Chi phí đỗ xe trong hợp đồng thuê?"))

        assertEquals(true, viewModel.uiState.value.messages[2].content.contains("không phải tư vấn pháp lý"))
    }

    @Test
    fun retryReplacesTheRepositoryErrorWithFallbackAnswer() = runTest {
        val viewModel = viewModel()
        openScreen(viewModel)
        viewModel.onEvent(ChatbotUiEvent.SuggestionClicked("__demo_retryable_error__"))
        advanceUntilIdle()

        viewModel.onEvent(ChatbotUiEvent.ErrorRetryClicked(viewModel.uiState.value.messages[2].id))
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.messages[2].error)
        assertEquals(
            "[DEMO] Tôi đã ghi nhận câu hỏi về hợp đồng lao động. Phản hồi này chỉ minh họa giao diện, chưa được phân tích bởi AI và không phải tư vấn pháp lý.",
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

    @Test
    fun failedSendIsSavedWithItsRetryQuestion() = runTest {
        val stored = FakeChatbotRepository()
        val failing = object : com.example.lagallens.domain.repository.ChatbotRepository by stored {
            override suspend fun sendForSession(
                sessionKey: String?, conversationId: String, message: String,
                attachment: com.example.lagallens.domain.model.chatbot.ChatbotAttachment?
            ): com.example.lagallens.domain.model.chatbot.ChatbotReply = throw IllegalStateException("offline")
        }
        val viewModel = viewModel(failing)
        openScreen(viewModel)
        viewModel.onEvent(ChatbotUiEvent.MessageChanged("Điều khoản thanh toán thế nào?"))

        viewModel.onEvent(ChatbotUiEvent.SendClicked)
        advanceUntilIdle()

        val transcript = stored.getConversations().single().messages
        val error = transcript.last()
        assertEquals(false, error.isUser)
        assertEquals("Điều khoản thanh toán thế nào?", error.retryQuestion)
        assertEquals(true, error.errorRetryLabel != null)
    }

    @Test
    fun checkingAgainMarksTheCardUntilTheServerAnswers() = runTest {
        val stored = FakeChatbotRepository()
        val answer = kotlinx.coroutines.CompletableDeferred<com.example.lagallens.domain.model.chatbot.ChatbotReply>()
        val pending = com.example.lagallens.domain.model.chatbot.ChatbotReply.RetryableError("Chưa có câu trả lời", "Kiểm tra lại")
        val waiting = object : com.example.lagallens.domain.repository.ChatbotRepository by stored {
            override suspend fun sendForSession(
                sessionKey: String?, conversationId: String, message: String,
                attachment: com.example.lagallens.domain.model.chatbot.ChatbotAttachment?
            ) = pending
            override suspend fun retryForSession(
                sessionKey: String?, conversationId: String, message: String,
                attachment: com.example.lagallens.domain.model.chatbot.ChatbotAttachment?
            ) = answer.await()
        }
        val viewModel = viewModel(waiting)
        openScreen(viewModel)
        viewModel.onEvent(ChatbotUiEvent.MessageChanged("Lương?"))
        viewModel.onEvent(ChatbotUiEvent.SendClicked)
        advanceUntilIdle()
        val card = viewModel.uiState.value.messages.last()

        viewModel.onEvent(ChatbotUiEvent.ErrorRetryClicked(card.id))
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.messages.last().error?.isRetrying)
        answer.complete(com.example.lagallens.domain.model.chatbot.ChatbotReply.Answer("10 triệu."))
        advanceUntilIdle()
        val replaced = viewModel.uiState.value.messages.last()
        assertEquals("10 triệu.", replaced.content)
        assertEquals(null, replaced.error)
    }

    @Test
    fun draftingCannotStartWhenItIsDisabled() = runTest {
        val viewModel = ChatbotViewModel(
            openConversation = OpenChatbotConversationUseCase(FakeChatbotRepository()),
            sendMessage = SendChatbotMessageUseCase(FakeChatbotRepository()),
            retryMessage = RetryChatbotMessageUseCase(FakeChatbotRepository()),
            draftAi = { com.example.lagallens.TestDraftAiRepository() },
            isContractDraftingEnabled = false
        )
        openScreen(viewModel)
        advanceUntilIdle()

        viewModel.onEvent(ChatbotUiEvent.NewDraftClicked("Tạo hợp đồng mới", "Bạn cần thông tin gì?"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isContractDraftingEnabled)
        assertEquals(false, state.isDraftConversation)
        assertEquals(true, state.messages.none { it.content == "Tạo hợp đồng mới" })
    }

    private fun viewModel(repository: com.example.lagallens.domain.repository.ChatbotRepository = FakeChatbotRepository()): ChatbotViewModel {
        return ChatbotViewModel(
            openConversation = OpenChatbotConversationUseCase(repository),
            sendMessage = SendChatbotMessageUseCase(repository),
            retryMessage = RetryChatbotMessageUseCase(repository),
            draftAi = { com.example.lagallens.TestDraftAiRepository() }
        )
    }

    private fun openScreen(viewModel: ChatbotViewModel) {
        viewModel.onEvent(
            ChatbotUiEvent.ScreenOpened(
                contractName = "HĐ Lao động mẫu",
                contractType = "Hợp đồng lao động"
            )
        )
    }
}
