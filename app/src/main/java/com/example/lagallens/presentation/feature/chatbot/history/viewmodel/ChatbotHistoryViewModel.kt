package com.example.lagallens.presentation.feature.chatbot.history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiEffect
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiEvent
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatbotHistoryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatbotHistoryUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ChatbotHistoryUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: ChatbotHistoryUiEvent) {
        when (event) {
            is ChatbotHistoryUiEvent.ScreenOpened -> _uiState.update { it.copy(conversations = event.conversations) }
            ChatbotHistoryUiEvent.BackClicked -> emitEffect(ChatbotHistoryUiEffect.NavigateBack)
            ChatbotHistoryUiEvent.SearchClicked -> emitEffect(
                ChatbotHistoryUiEffect.ShowMessage(R.string.chatbot_history_search_unavailable)
            )
        }
    }

    private fun emitEffect(effect: ChatbotHistoryUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
