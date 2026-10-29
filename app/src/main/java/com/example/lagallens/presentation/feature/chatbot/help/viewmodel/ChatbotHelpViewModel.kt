package com.example.lagallens.presentation.feature.chatbot.help.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.chatbot.help.contract.ChatbotHelpUiEvent
import com.example.lagallens.presentation.feature.chatbot.help.contract.ChatbotHelpUiEffect
import com.example.lagallens.presentation.feature.chatbot.help.contract.ChatbotHelpUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatbotHelpViewModel : ViewModel() {
    private val state = MutableStateFlow(ChatbotHelpUiState())
    val uiState = state.asStateFlow()
    private val effects = Channel<ChatbotHelpUiEffect>(Channel.BUFFERED)
    val uiEffect = effects.receiveAsFlow()
    fun onEvent(event: ChatbotHelpUiEvent) {
        when (event) {
            ChatbotHelpUiEvent.BackClicked -> viewModelScope.launch { effects.send(ChatbotHelpUiEffect.Close) }
            is ChatbotHelpUiEvent.FaqClicked -> state.update { it.copy(expandedFaq = if (it.expandedFaq == event.index) null else event.index) }
            is ChatbotHelpUiEvent.QuestionClicked -> viewModelScope.launch { effects.send(ChatbotHelpUiEffect.ReturnQuestion(event.question)) }
        }
    }
}
