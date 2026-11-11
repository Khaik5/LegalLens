package com.example.lagallens.presentation.feature.chatbot.history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryConversation
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiEffect
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiEvent
import com.example.lagallens.presentation.feature.chatbot.history.contract.ChatbotHistoryUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import com.example.lagallens.domain.repository.ChatbotRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.lagallens.presentation.common.utils.toFailureMessage

class ChatbotHistoryViewModel(private val repository: ChatbotRepository? = null) : ViewModel() {
    private val ownerSession = repository?.currentSessionKey()
    private val pendingJobs = mutableSetOf<Job>()
    private var allConversations: List<ChatbotHistoryConversation> = emptyList()
    private val _uiState = MutableStateFlow(ChatbotHistoryUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<ChatbotHistoryUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private fun isCurrent() = repository == null || repository.currentSessionKey() == ownerSession

    init {
        repository?.let { source ->
            viewModelScope.launch {
                source.observeSessionKeys().collect { session ->
                    if (session != ownerSession) {
                        pendingJobs.toList().forEach { it.cancel() }
                        allConversations = emptyList()
                        _uiState.value = ChatbotHistoryUiState()
                    }
                }
            }
        }
    }

    private fun launchForOwner(block: suspend () -> Unit) {
        if (!isCurrent()) return
        val job = viewModelScope.launch { if (isCurrent()) block() }
        pendingJobs.add(job)
        job.invokeOnCompletion { pendingJobs.remove(job) }
    }

    fun onEvent(event: ChatbotHistoryUiEvent) {
        if (!isCurrent()) return
        when (event) {
            is ChatbotHistoryUiEvent.ScreenOpened -> {
                if (repository == null) {
                    allConversations = event.conversations
                    filter(_uiState.value.query)
                } else launchForOwner {
                    runCatching { requireNotNull(repository).conversationsForSession(ownerSession) }.onSuccess { records ->
                        if (!isCurrent()) return@onSuccess
                        allConversations = records.map { record ->
                            ChatbotHistoryConversation(
                                id = record.conversationId,
                                contractName = record.contractName,
                                contractType = record.contractType,
                                timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(record.updatedAtEpochMillis)),
                                messageCount = "${record.messages.size} tin nhắn",
                                preview = record.messages.lastOrNull()?.content.orEmpty(),
                                contractId = record.contractId
                            )
                        }
                        filter(_uiState.value.query)
                    }.onFailure {
                        if (it is CancellationException) throw it
                        if (isCurrent()) emitEffect(ChatbotHistoryUiEffect.ShowMessage(it.toFailureMessage()))
                    }
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
        launchForOwner { if (isCurrent()) _uiEffect.send(effect) }
    }
}
