package com.example.lagallens.presentation.feature.chatbot.draft.save

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.domain.model.DraftStage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SaveDraftCopyStatus { FORM, LOADING, ERROR, SUCCESS }
data class SaveDraftCopyUiState(
    val status: SaveDraftCopyStatus = SaveDraftCopyStatus.FORM,
    val name: String = "",
    val acknowledged: Boolean = true
)

class SaveDraftCopyViewModel(application: Application) : AndroidViewModel(application) {
    private val state = MutableStateFlow(SaveDraftCopyUiState())
    val uiState = state.asStateFlow()
    private var version: String? = null
    private var content: String = ""
    private var contractId: String? = null
    private var sourceDraftId: String? = null

    fun setDraft(value: String, savedContent: String, linkedContractId: String?, parentDraftId: String?) {
        if (version == value && content == savedContent && contractId == linkedContractId && sourceDraftId == parentDraftId) return
        version = value
        content = savedContent
        contractId = linkedContractId
        sourceDraftId = parentDraftId
        state.update { it.copy(name = getApplication<Application>().getString(R.string.chatbot_draft_document_name, value)) }
    }

    fun setName(value: String) = state.update { it.copy(name = value) }
    fun setAcknowledged(value: Boolean) = state.update { it.copy(acknowledged = value) }

    fun save() {
        if (!state.value.acknowledged || state.value.name.isBlank() || state.value.status == SaveDraftCopyStatus.LOADING) return
        state.update { it.copy(status = SaveDraftCopyStatus.LOADING) }
        viewModelScope.launch {
            val saved = try {
                withContext(Dispatchers.IO) {
                    val stage = DraftStage.fromLegacyKey(version.orEmpty()) ?: DraftStage.ORIGINAL
                    getApplication<LegalLensApplication>().draftRepository.saveDraft(
                        stage = stage,
                        title = state.value.name,
                        content = content,
                        contractId = contractId,
                        sourceDraftId = sourceDraftId
                    )
                }
                true
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                false
            }
            state.update { it.copy(status = if (saved) SaveDraftCopyStatus.SUCCESS else SaveDraftCopyStatus.ERROR) }
        }
    }

    fun retry() {
        if (state.value.status == SaveDraftCopyStatus.ERROR) save()
    }

    fun returnToForm() = state.update { it.copy(status = SaveDraftCopyStatus.FORM) }
}
