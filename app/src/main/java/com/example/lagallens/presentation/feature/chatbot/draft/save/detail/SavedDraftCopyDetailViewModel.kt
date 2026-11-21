package com.example.lagallens.presentation.feature.chatbot.draft.save.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.repository.DraftRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Turns a saved draft into a PDF contract in the user's contract list. */
class SavedDraftCopyDetailViewModel(
    private val ownerSession: String?,
    private val currentSession: () -> String?,
    private val drafts: () -> DraftRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SavedDraftCopyDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SavedDraftCopyDetailUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: SavedDraftCopyDetailUiEvent) {
        if (ownerSession != currentSession()) return
        when (event) {
            is SavedDraftCopyDetailUiEvent.SaveAsContractClicked -> saveAsContract(event.draftId)
        }
    }

    private fun saveAsContract(draftId: String) {
        if (_uiState.value.saveAsContractStatus != SaveAsContractStatus.IDLE) return
        _uiState.update { it.copy(saveAsContractStatus = SaveAsContractStatus.SAVING) }
        viewModelScope.launch {
            val contractId = try {
                drafts().saveAsContract(draftId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                null
            }
            if (ownerSession != currentSession()) return@launch
            if (contractId != null) {
                _uiState.update { it.copy(saveAsContractStatus = SaveAsContractStatus.SAVED) }
                _uiEffect.emit(SavedDraftCopyDetailUiEffect.ShowMessage(R.string.draft_saved_contract_success))
            } else {
                // The same request is allowed again: the repository reuses its idempotency key.
                _uiState.update { it.copy(saveAsContractStatus = SaveAsContractStatus.IDLE) }
                _uiEffect.emit(SavedDraftCopyDetailUiEffect.ShowMessage(R.string.draft_saved_contract_failed))
            }
        }
    }

    class Factory(
        private val owner: String?,
        private val current: () -> String?,
        private val drafts: () -> DraftRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SavedDraftCopyDetailViewModel(owner, current, drafts) as T
    }
}
