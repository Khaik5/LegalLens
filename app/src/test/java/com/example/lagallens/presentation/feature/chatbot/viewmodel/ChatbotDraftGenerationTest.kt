package com.example.lagallens.presentation.feature.chatbot.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.data.repository.FakeChatbotRepository
import com.example.lagallens.domain.usecase.OpenChatbotConversationUseCase
import com.example.lagallens.domain.usecase.RetryChatbotMessageUseCase
import com.example.lagallens.domain.usecase.SendChatbotMessageUseCase
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEvent
import com.example.lagallens.presentation.feature.chatbot.contract.DraftGenerationStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatbotDraftGenerationTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun failureRetainsMessagesAndRetryCompletesSample() = runTest {
        val model = prepareDraft(previewError = true)
        val messages = model.uiState.value.messages
        model.onEvent(ChatbotUiEvent.CreateDraftClicked)
        assertEquals(DraftGenerationStatus.LOADING, model.uiState.value.draftGenerationStatus)
        advanceUntilIdle()
        assertEquals(DraftGenerationStatus.ERROR, model.uiState.value.draftGenerationStatus)
        assertEquals(messages, model.uiState.value.messages)
        model.onEvent(ChatbotUiEvent.RetryDraftCreationClicked)
        assertEquals(DraftGenerationStatus.LOADING, model.uiState.value.draftGenerationStatus)
        advanceUntilIdle()
        assertEquals(DraftGenerationStatus.SUCCESS, model.uiState.value.draftGenerationStatus)
        assertEquals(messages, model.uiState.value.messages)
    }

    @Test
    fun startingNewChatCancelsPendingGeneration() = runTest {
        val model = prepareDraft(previewError = false)
        model.onEvent(ChatbotUiEvent.CreateDraftClicked)
        model.onEvent(ChatbotUiEvent.NewChatClicked)
        advanceUntilIdle()
        assertEquals(DraftGenerationStatus.NONE, model.uiState.value.draftGenerationStatus)
        assertTrue(model.uiState.value.isNewConversation)
    }

    @Test
    fun editingAfterFailureKeepsConversation() = runTest {
        val model = prepareDraft(previewError = true)
        val messages = model.uiState.value.messages
        model.onEvent(ChatbotUiEvent.CreateDraftClicked)
        advanceUntilIdle()
        model.onEvent(ChatbotUiEvent.EditDraftClicked)
        assertEquals(DraftGenerationStatus.NONE, model.uiState.value.draftGenerationStatus)
        assertEquals(messages, model.uiState.value.messages)
    }

    private fun prepareDraft(previewError: Boolean): ChatbotViewModel {
        val repository = FakeChatbotRepository()
        val model = ChatbotViewModel(OpenChatbotConversationUseCase(repository), SendChatbotMessageUseCase(repository), RetryChatbotMessageUseCase(repository))
        model.onEvent(ChatbotUiEvent.ScreenOpened("HĐ Thuê VP Bitexco", "Thuê văn phòng", startNewConversation = true, previewDraftError = previewError))
        model.onEvent(ChatbotUiEvent.NewDraftClicked("Yêu cầu mẫu", "Cần bổ sung gì?"))
        model.onEvent(ChatbotUiEvent.DraftDetailsSubmitted("Chi tiết mẫu", "Đã ghi nhận"))
        model.onEvent(ChatbotUiEvent.ReviewDraftClicked)
        return model
    }
}
