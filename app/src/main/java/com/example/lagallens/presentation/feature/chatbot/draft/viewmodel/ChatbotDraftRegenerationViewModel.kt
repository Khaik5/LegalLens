package com.example.lagallens.presentation.feature.chatbot.draft.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.presentation.feature.chatbot.draft.DraftContentFactory
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftRegenerationStatus
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftRegenerationUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftRegenerationUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatbotDraftRegenerationViewModel(application: Application) : AndroidViewModel(application) {
    private val state = MutableStateFlow(ChatbotDraftRegenerationUiState())
    val uiState = state.asStateFlow()
    private var previewFailure = false
    private var sourceVersion = "D1"
    private var sourceDraftId: String? = null
    private var contractId: String? = null

    fun onEvent(event: ChatbotDraftRegenerationUiEvent) {
        when (event) {
            is ChatbotDraftRegenerationUiEvent.Opened -> if (!state.value.isInitialized) {
                previewFailure = event.previewFailure
                sourceVersion = event.sourceVersion
                sourceDraftId = event.sourceDraftId
                contractId = event.contractId
                state.update { it.copy(isInitialized = true, status = ChatbotDraftRegenerationStatus.LOADING) }
                generate()
            }
            ChatbotDraftRegenerationUiEvent.RetryClicked -> if (state.value.status == ChatbotDraftRegenerationStatus.ERROR) {
                previewFailure = false
                state.update { it.copy(status = ChatbotDraftRegenerationStatus.LOADING) }
                generate()
            }
        }
    }

    private fun generate() {
        viewModelScope.launch {
            val shouldFail = previewFailure
            previewFailure = false
            try {
                delay(GENERATION_DELAY_MS)
                if (shouldFail) error("Preview failure")
                val draft = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val repository = getApplication<LegalLensApplication>().draftRepository
                    val sourceStage = DraftStage.fromLegacyKey(sourceVersion) ?: DraftStage.ORIGINAL
                    val source = sourceDraftId?.let { repository.getDraft(it) }
                        ?: repository.getDraft(sourceVersion)
                        ?: repository.saveDraft(
                            sourceStage,
                            context.getString(R.string.chatbot_draft_document_name, sourceVersion),
                            DraftContentFactory.create(context, sourceStage),
                            contractId
                        )
                    repository.createCopy(
                        source.draftId,
                        context.getString(R.string.chatbot_draft_document_name, DraftStage.REGENERATED.legacyKey),
                        DraftStage.REGENERATED
                    ) ?: error("Regenerated draft could not be created")
                }
                state.update { it.copy(status = ChatbotDraftRegenerationStatus.SUCCESS, draftId = draft.draftId) }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                state.update { it.copy(status = ChatbotDraftRegenerationStatus.ERROR) }
            }
        }
    }

    private companion object {
        const val GENERATION_DELAY_MS = 1800L
    }
}
