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
    private val app = application as LegalLensApplication
    private val ownerSession = app.currentSessionKey()
    private val state = MutableStateFlow(SaveDraftCopyUiState())
    val uiState = state.asStateFlow()
    private var version: String? = null
    private var content: String = ""
    private var contractId: String? = null
    private var sourceDraftId: String? = null

    init {
        viewModelScope.launch {
            app.observeSessionKeys().collect { session ->
                if (session != ownerSession) {
                    version = null
                    content = ""
                    contractId = null
                    sourceDraftId = null
                    state.value = SaveDraftCopyUiState()
                }
            }
        }
    }

    fun setDraft(value: String, savedContent: String, linkedContractId: String?, parentDraftId: String?) {
        if (ownerSession != app.currentSessionKey()) return
        if (version == value && content == savedContent && contractId == linkedContractId && sourceDraftId == parentDraftId) return
        version = value
        content = savedContent
        contractId = linkedContractId
        sourceDraftId = parentDraftId
        state.update { it.copy(name = getApplication<Application>().getString(R.string.chatbot_draft_document_name, value)) }
    }

    fun setName(value: String) { if (ownerSession == app.currentSessionKey()) state.update { it.copy(name = value) } }
    fun setAcknowledged(value: Boolean) { if (ownerSession == app.currentSessionKey()) state.update { it.copy(acknowledged = value) } }

    fun save() {
        if (ownerSession != app.currentSessionKey()) return
        if (!state.value.acknowledged || state.value.name.isBlank() || state.value.status == SaveDraftCopyStatus.LOADING) return
        val repository = try { app.draftRepository.forSession(ownerSession) } catch (_: Exception) { return }
        val draftVersion = version
        val draftName = state.value.name
        val draftContent = content
        val linkedContract = contractId
        val parentDraft = sourceDraftId
        state.update { it.copy(status = SaveDraftCopyStatus.LOADING) }
        viewModelScope.launch {
            val saved = try {
                withContext(Dispatchers.IO) {
                    val stage = DraftStage.fromLegacyKey(draftVersion.orEmpty()) ?: DraftStage.ORIGINAL
                    repository.saveDraft(
                        stage = stage,
                        title = draftName,
                        content = draftContent,
                        contractId = linkedContract,
                        sourceDraftId = parentDraft,
                        isSaved = true
                    )
                }
                true
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                false
            }
            if (ownerSession == app.currentSessionKey()) state.update { it.copy(status = if (saved) SaveDraftCopyStatus.SUCCESS else SaveDraftCopyStatus.ERROR) }
        }
    }

    fun retry() {
        if (state.value.status == SaveDraftCopyStatus.ERROR) save()
    }

    fun returnToForm() = state.update { it.copy(status = SaveDraftCopyStatus.FORM) }
}
