package com.example.lagallens.presentation.feature.contracts.detail.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.presentation.feature.contracts.contract.ContractListItem
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractAssistantAction
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiEffect
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiEvent
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.lagallens.presentation.feature.contracts.contract.toContractListItem

class ContractDetailViewModel(
    private val repository: com.example.lagallens.domain.repository.ContractRepository,
    private val relatedData: com.example.lagallens.domain.usecase.SyncContractRelatedDataUseCase? = null
) : ViewModel() {
    private var activeSession = repository.activeSessionKey()
    private var currentContractId: String? = null
    private var currentContract: EmploymentContract? = null
    private val _uiState = MutableStateFlow(ContractDetailUiState())
    val uiState = _uiState.asStateFlow()
    private val _uiEffect = Channel<ContractDetailUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observeSessionKeys().collect { session ->
                if (session != activeSession) {
                    activeSession = session
                    currentContractId = null
                    currentContract = null
                    _uiState.value = ContractDetailUiState()
                }
            }
        }
    }

    fun loadContract(contractId: String?) {
        if (activeSession != repository.activeSessionKey()) {
            activeSession = repository.activeSessionKey()
            currentContractId = null
            currentContract = null
            _uiState.value = ContractDetailUiState()
        }
        if (contractId.isNullOrBlank() || currentContractId == contractId) return
        currentContractId = contractId
        val session = activeSession
        viewModelScope.launch {
            try {
                val contract = repository.getContract(contractId)
                if (session != repository.activeSessionKey() || currentContractId != contractId) return@launch
                if (contract == null) {
                    emitEffect(ContractDetailUiEffect.NavigateBack)
                    return@launch
                }
                currentContract = contract
                _uiState.value = ContractDetailUiState.from(contract.toContractListItem(""), contract)
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                if (session != repository.activeSessionKey() || currentContractId != contractId) return@launch
                emitEffect(ContractDetailUiEffect.ShowMessage(R.string.auth_backend_not_configured))
            }
        }
    }

    fun onEvent(event: ContractDetailUiEvent) {
        if (activeSession != repository.activeSessionKey()) {
            activeSession = repository.activeSessionKey()
            currentContractId = null
            currentContract = null
            _uiState.value = ContractDetailUiState()
            return
        }
        when (event) {
            ContractDetailUiEvent.BackClicked -> emitEffect(ContractDetailUiEffect.NavigateBack)
            is ContractDetailUiEvent.AssistantActionClicked -> {
                val state = _uiState.value
                when (event.action) {
                    ContractAssistantAction.VIEW_DOCUMENT -> currentContract?.let { emitEffect(ContractDetailUiEffect.NavigateToDocument(it.id, it.fileUri, state.renamedTitle ?: it.title)) }
                    ContractAssistantAction.COMPARE -> currentContractId?.let { emitEffect(ContractDetailUiEffect.NavigateToCompare(it)) }
                    ContractAssistantAction.OCR_TEXT -> emitEffect(ContractDetailUiEffect.NavigateToOcrText(
                        state.documentId, state.contractId, _uiState.value.renamedTitle ?: currentContract?.title.orEmpty(),
                        state.fileFormat, state.documentText, activeSession
                    ))
                    ContractAssistantAction.ANALYZE -> currentContract?.let { contract ->
                        if (contract.id.isNotBlank() && contract.documentId.isNotBlank())
                            emitEffect(ContractDetailUiEffect.NavigateToAnalysis(state.renamedTitle ?: contract.title, contract.fileName, contract.id, contract.documentId))
                    }
                    ContractAssistantAction.CHAT -> currentContractId?.let {
                        emitEffect(ContractDetailUiEffect.NavigateToChatbot(state.renamedTitle.orEmpty(), state.contractTypeRes, it))
                    }
                    ContractAssistantAction.EXPORT -> emitEffect(ContractDetailUiEffect.ShowExportSheet)
                }
            }
            ContractDetailUiEvent.RenameClicked -> emitEffect(ContractDetailUiEffect.ShowRenameDialog)
            is ContractDetailUiEvent.RenameConfirmed -> {
                val id = currentContractId ?: return
                val session = activeSession
                viewModelScope.launch {
                    val renamed = try {
                        repository.renameContract(id, event.name)
                    } catch (cancellationException: CancellationException) {
                        throw cancellationException
                    } catch (_: Exception) {
                        emitEffect(ContractDetailUiEffect.ShowMessage(R.string.contract_action_failed))
                        return@launch
                    }
                    if (session != repository.activeSessionKey() || currentContractId != id) return@launch
                    if (renamed) {
                        syncRelatedData { relatedData?.contractRenamed(id, event.name.trim()) }
                        currentContract = currentContract?.copy(title = event.name.trim())
                        _uiState.update { it.copy(renamedTitle = event.name.trim()) }
                    } else emitEffect(ContractDetailUiEffect.ShowMessage(R.string.contract_rename_empty))
                }
            }
            ContractDetailUiEvent.DeleteClicked -> emitEffect(ContractDetailUiEffect.ShowDeleteConfirmation)
            ContractDetailUiEvent.DeleteConfirmed -> {
                val id = currentContractId ?: return
                val session = activeSession
                viewModelScope.launch {
                    val deleted = try {
                        repository.deleteContract(id)
                    } catch (cancellationException: CancellationException) {
                        throw cancellationException
                    } catch (_: Exception) {
                        emitEffect(ContractDetailUiEffect.ShowMessage(R.string.contract_action_failed))
                        return@launch
                    }
                    if (session != repository.activeSessionKey() || currentContractId != id) return@launch
                    if (deleted) {
                        syncRelatedData { relatedData?.contractDeleted(session, id) }
                        emitEffect(ContractDetailUiEffect.NavigateBack)
                    }
                    else emitEffect(ContractDetailUiEffect.ShowMessage(R.string.contract_detail_delete_unavailable))
                }
            }
        }
    }

    /** The contract change already succeeded, so a failure here is reported without undoing it. */
    private suspend fun syncRelatedData(action: suspend () -> Unit) {
        try {
            action()
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Exception) {
            emitEffect(ContractDetailUiEffect.ShowMessage(R.string.contract_action_failed))
        }
    }

    private fun emitEffect(effect: ContractDetailUiEffect) {
        val session = activeSession
        viewModelScope.launch { if (session == repository.activeSessionKey()) _uiEffect.send(effect) }
    }

    class Factory(
        private val repository: com.example.lagallens.domain.repository.ContractRepository,
        private val relatedData: com.example.lagallens.domain.usecase.SyncContractRelatedDataUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ContractDetailViewModel::class.java))
            return ContractDetailViewModel(repository, relatedData) as T
        }
    }
}
