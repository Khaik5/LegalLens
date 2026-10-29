package com.example.lagallens.presentation.feature.chatbot.draft.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.domain.model.DraftStage
import com.example.lagallens.presentation.feature.chatbot.draft.DraftContentFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import com.example.lagallens.presentation.feature.chatbot.draft.contract.CopyDraftVersionStatus
import com.example.lagallens.presentation.feature.chatbot.draft.contract.CopyDraftVersionUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.CopyDraftVersionUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CopyDraftVersionViewModel(application: Application) : AndroidViewModel(application) {
    private val state = MutableStateFlow(CopyDraftVersionUiState())
    val uiState = state.asStateFlow()
    private var initialized = false
    private var previewFailure = false
    private var sourceDraftId: String? = null
    private var contractId: String? = null

    fun onEvent(event: CopyDraftVersionUiEvent) {
        when (event) {
            is CopyDraftVersionUiEvent.Opened -> if (!initialized) {
                initialized = true
                previewFailure = event.previewFailure
                sourceDraftId = event.sourceDraftId
                contractId = event.contractId
                copyVersion()
            }
            CopyDraftVersionUiEvent.RetryClicked -> if (state.value.status == CopyDraftVersionStatus.ERROR) {
                previewFailure = false
                state.update { it.copy(status = CopyDraftVersionStatus.LOADING) }
                copyVersion()
            }
        }
    }

    private fun copyVersion() {
        viewModelScope.launch {
            val shouldFail = previewFailure
            previewFailure = false
            try {
                delay(COPY_DELAY_MS)
                if (shouldFail) error("Preview failure")
                val draft = withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val repository = getApplication<LegalLensApplication>().draftRepository
                    val source = sourceDraftId?.let { repository.getDraft(it) }
                        ?: repository.getDraft(DraftStage.ORIGINAL.legacyKey)
                        ?: repository.saveDraft(
                            DraftStage.ORIGINAL,
                            context.getString(R.string.chatbot_draft_document_name, DraftStage.ORIGINAL.legacyKey),
                            DraftContentFactory.create(context, DraftStage.ORIGINAL),
                            contractId
                        )
                    repository.createCopy(
                        source.draftId,
                        context.getString(R.string.chatbot_draft_document_name, DraftStage.COPY.legacyKey),
                        DraftStage.COPY
                    ) ?: error("Draft copy could not be created")
                }
                state.update { it.copy(status = CopyDraftVersionStatus.SUCCESS, draftId = draft.draftId) }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                state.update { it.copy(status = CopyDraftVersionStatus.ERROR) }
            }
        }
    }

    private companion object {
        const val COPY_DELAY_MS = 1400L
    }
}
