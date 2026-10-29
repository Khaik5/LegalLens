package com.example.lagallens.presentation.feature.chatbot.draft.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.presentation.feature.chatbot.draft.DraftContentFactory
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiApplyStatus
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiApplyUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.ChatbotDraftAiApplyUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatbotDraftAiApplyViewModel(application: Application) : AndroidViewModel(application) {
    private val state = MutableStateFlow(ChatbotDraftAiApplyUiState())
    val uiState = state.asStateFlow()
    private var initialized = false
    private var previewFailure = false
    private var sourceDraftId: String? = null
    private var contractId: String? = null

    fun onEvent(event: ChatbotDraftAiApplyUiEvent) {
        when (event) {
            is ChatbotDraftAiApplyUiEvent.Opened -> if (!initialized) {
                initialized = true
                previewFailure = event.previewFailure
                sourceDraftId = event.sourceDraftId
                contractId = event.contractId
                applyProposal()
            }
            ChatbotDraftAiApplyUiEvent.RetryClicked -> if (state.value.status == ChatbotDraftAiApplyStatus.ERROR) {
                previewFailure = false
                state.update { it.copy(status = ChatbotDraftAiApplyStatus.APPLYING) }
                applyProposal()
            }
        }
    }

    private fun applyProposal() {
        viewModelScope.launch {
            val shouldFail = previewFailure
            previewFailure = false
            if (shouldFail) {
                delay(APPLY_DELAY_MS)
                state.update { it.copy(status = ChatbotDraftAiApplyStatus.ERROR) }
                return@launch
            }
            state.update { it.copy(status = ChatbotDraftAiApplyStatus.APPLYING) }
            try {
                delay(APPLY_DELAY_MS)
                val draft = withContext(Dispatchers.IO) {
                    val repository = getApplication<LegalLensApplication>().draftRepository
                    val source = sourceDraftId?.let { repository.getDraft(it) }
                        ?: repository.getDraft(DraftStage.ORIGINAL.legacyKey)
                        ?: repository.saveDraft(
                            DraftStage.ORIGINAL,
                            getApplication<Application>().getString(R.string.chatbot_draft_document_name, DraftStage.ORIGINAL.legacyKey),
                            DraftContentFactory.create(getApplication(), DraftStage.ORIGINAL),
                            contractId
                        )
                    repository.saveDraft(
                        stage = DraftStage.AI_PROPOSAL,
                        title = getApplication<Application>().getString(R.string.chatbot_draft_document_name, DraftStage.AI_PROPOSAL.legacyKey),
                        content = DraftContentFactory.applyAiProposal(getApplication(), source.content),
                        contractId = source.contractId ?: contractId,
                        sourceDraftId = source.draftId
                    )
                }
                state.update { it.copy(status = ChatbotDraftAiApplyStatus.SUCCESS, draftId = draft.draftId) }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                state.update { it.copy(status = ChatbotDraftAiApplyStatus.ERROR) }
            }
        }
    }

    private companion object {
        const val APPLY_DELAY_MS = 1400L
    }
}
