package com.example.lagallens.presentation.feature.contracts.detail.export.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.usecase.GetDocumentFileUseCase
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportStatus
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportUiEffect
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportUiEvent
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Exports the contract's original uploaded file (PDF, Word or image), not the AI analysis. */
class ContractExportViewModel(
    private val getDocumentFile: GetDocumentFileUseCase,
    private val contractId: String,
    private val sessionKey: String
) : ViewModel() {
    private enum class ExportAction { SAVE, SHARE }

    private val state = MutableStateFlow(ContractExportUiState())
    val uiState = state.asStateFlow()
    private val effects = Channel<ContractExportUiEffect>(Channel.BUFFERED)
    val uiEffect = effects.receiveAsFlow()
    private var lastAction: ExportAction? = null

    fun onEvent(event: ContractExportUiEvent) {
        when (event) {
            is ContractExportUiEvent.Opened -> if (!state.value.isInitialized) state.update {
                it.copy(title = event.title, metadata = event.metadata, isInitialized = true)
            }
            ContractExportUiEvent.ExportClicked -> export(ExportAction.SAVE)
            ContractExportUiEvent.ShareClicked -> export(ExportAction.SHARE)
            ContractExportUiEvent.RetryClicked -> lastAction?.let(::export)
            ContractExportUiEvent.CancelClicked -> viewModelScope.launch { effects.send(ContractExportUiEffect.Close) }
            is ContractExportUiEvent.DownloadReleased -> getDocumentFile.release(event.fileUri)
        }
    }

    private fun export(action: ExportAction) {
        if (state.value.status == ContractExportStatus.LOADING) return
        lastAction = action
        state.update { it.copy(status = ContractExportStatus.LOADING) }
        viewModelScope.launch {
            try {
                check(contractId.isNotBlank() && sessionKey.isNotBlank()) { "Missing contract or session" }
                val document = checkNotNull(getDocumentFile.findForContract(contractId, sessionKey)) { "No document" }
                val fileUri = getDocumentFile(document.id, sessionKey, forDownload = true)
                state.update { it.copy(status = ContractExportStatus.OPTIONS) }
                effects.send(
                    when (action) {
                        ExportAction.SAVE -> ContractExportUiEffect.SaveFile(fileUri, document.displayName, document.mimeType)
                        ExportAction.SHARE -> ContractExportUiEffect.ShareFile(fileUri, document.displayName, document.mimeType)
                    }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.update { it.copy(status = ContractExportStatus.ERROR) }
            }
        }
    }

    class Factory(
        private val getDocumentFile: GetDocumentFileUseCase,
        private val contractId: String,
        private val sessionKey: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ContractExportViewModel(getDocumentFile, contractId, sessionKey) as T
    }
}
