package com.example.lagallens.presentation.feature.chatbot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotMessage
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotMessageAuthor
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEffect
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiEvent
import com.example.lagallens.presentation.feature.chatbot.contract.ChatbotUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatbotViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ChatbotUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var nextMessageId = INITIAL_MESSAGE_ID
    private var localResponse = ""

    fun onEvent(event: ChatbotUiEvent) {
        when (event) {
            is ChatbotUiEvent.ScreenOpened -> openScreen(event)
            is ChatbotUiEvent.MessageChanged -> _uiState.update { it.copy(messageDraft = event.message) }
            ChatbotUiEvent.SendClicked -> sendMessage()
            ChatbotUiEvent.AttachmentClicked -> emitEffect(ChatbotUiEffect.ShowMessage(R.string.chatbot_attachment_unavailable))
            ChatbotUiEvent.BackClicked -> emitEffect(ChatbotUiEffect.NavigateBack)
        }
    }

    private fun openScreen(event: ChatbotUiEvent.ScreenOpened) {
        if (_uiState.value.messages.isNotEmpty()) return
        localResponse = event.localResponse
        _uiState.update {
            it.copy(
                contractName = event.contractName,
                contractType = event.contractType,
                messages = initialMessages(event)
            )
        }
    }

    private fun sendMessage() {
        val message = _uiState.value.messageDraft.trim()
        if (message.isBlank()) return
        _uiState.update { state ->
            state.copy(
                messageDraft = "",
                messages = state.messages + ChatbotMessage(
                    id = nextMessageId++,
                    author = ChatbotMessageAuthor.USER,
                    content = message
                ) + ChatbotMessage(
                    id = nextMessageId++,
                    author = ChatbotMessageAuthor.ASSISTANT,
                    content = localResponse
                )
            )
        }
    }

    private fun initialMessages(event: ChatbotUiEvent.ScreenOpened): List<ChatbotMessage> = listOf(
        ChatbotMessage(INITIAL_MESSAGE_ID, ChatbotMessageAuthor.ASSISTANT, event.initialGreeting),
        ChatbotMessage(INITIAL_MESSAGE_ID + 1, ChatbotMessageAuthor.USER, event.initialQuestion),
        ChatbotMessage(INITIAL_MESSAGE_ID + 2, ChatbotMessageAuthor.ASSISTANT, event.initialAnswer)
    ).also {
        nextMessageId = INITIAL_MESSAGE_ID + it.size
    }

    private fun emitEffect(effect: ChatbotUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    private companion object {
        const val INITIAL_MESSAGE_ID = 1L
    }
}
