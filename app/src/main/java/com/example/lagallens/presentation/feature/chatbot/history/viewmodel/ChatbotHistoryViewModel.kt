package com.example.lagallens.presentation.feature.chatbot.history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryConversation
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiEffect
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiEvent
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.lagallens.domain.repository.ChatbotRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatbotHistoryViewModel(private val repository: ChatbotRepository? = null) : ViewModel() {
    private var allConversations: List<ChatbotHistoryConversation> = emptyList()
    private val _uiState = MutableStateFlow(ChatbotHistoryUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ChatbotHistoryUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: ChatbotHistoryUiEvent) {
        when (event) {
            is ChatbotHistoryUiEvent.ScreenOpened -> {
                if (repository == null) {
                    allConversations = event.conversations
                    filter(_uiState.value.query)
                } else viewModelScope.launch {
                    runCatching { repository.getConversations() }.onSuccess { records ->
                        allConversations = records.map { record ->
                            ChatbotHistoryConversation(
                                id = record.conversationId,
                                contractName = record.contractName,
                                timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(record.updatedAtEpochMillis)),
                                messageCount = "${record.messages.size} tin nhắn",
                                preview = record.messages.lastOrNull()?.content.orEmpty(),
                                contractId = record.contractId
                            )
                        }
                        filter(_uiState.value.query)
                    }.onFailure { emitEffect(ChatbotHistoryUiEffect.ShowMessage(com.example.lagallens.R.string.auth_backend_not_configured)) }
                }
            }
            ChatbotHistoryUiEvent.BackClicked -> emitEffect(ChatbotHistoryUiEffect.NavigateBack)
            ChatbotHistoryUiEvent.SearchClicked -> _uiState.update { it.copy(isSearchVisible = true) }
            is ChatbotHistoryUiEvent.QueryChanged -> filter(event.query)
            is ChatbotHistoryUiEvent.ConversationClicked -> emitEffect(ChatbotHistoryUiEffect.OpenConversation(event.conversation))
        }
    }

    private fun filter(query: String) {
        val value = query.trim()
        _uiState.update { state -> state.copy(query = query, conversations = allConversations.filter {
            it.contractName.contains(value, ignoreCase = true) || it.preview.contains(value, ignoreCase = true)
        }) }
    }

    private fun emitEffect(effect: ChatbotHistoryUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
