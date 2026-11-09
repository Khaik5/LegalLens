package com.example.lagallens.presentation.feature.chatbot.draft.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import kotlinx.coroutines.CancellationException
import com.example.lagallens.presentation.feature.chatbot.draft.contract.CopyDraftVersionStatus
import com.example.lagallens.presentation.feature.chatbot.draft.contract.CopyDraftVersionUiEvent
import com.example.lagallens.presentation.feature.chatbot.draft.contract.CopyDraftVersionUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CopyDraftVersionViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LegalLensApplication
    private val ownerSession = app.currentSessionKey()
    private val state = MutableStateFlow(CopyDraftVersionUiState())
    val uiState = state.asStateFlow()
    private var initialized = false
    private var previewFailure = false
    private var sourceDraftId: String? = null
    private var contractId: String? = null

    init {
        viewModelScope.launch {
            app.observeSessionKeys().collect { session ->
                if (session != ownerSession) {
                    sourceDraftId = null
                    contractId = null
                    state.value = CopyDraftVersionUiState()
                }
            }
        }
    }

    fun onEvent(event: CopyDraftVersionUiEvent) {
        if (ownerSession != app.currentSessionKey()) return
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
        val repository = try { app.draftAiRepository(ownerSession) } catch (_: Exception) { return }
        val capturedSourceId = sourceDraftId
        val capturedContractId = contractId
        viewModelScope.launch {
            val shouldFail = previewFailure
            previewFailure = false
            try {
                if (shouldFail) error("Preview failure")
                val draft = repository.copyVersion(capturedSourceId, capturedContractId)
                if (ownerSession == app.currentSessionKey()) state.update { it.copy(status = CopyDraftVersionStatus.SUCCESS, draftId = draft.draftId) }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                if (ownerSession == app.currentSessionKey()) state.update { it.copy(status = CopyDraftVersionStatus.ERROR) }
            }
        }
    }

}
