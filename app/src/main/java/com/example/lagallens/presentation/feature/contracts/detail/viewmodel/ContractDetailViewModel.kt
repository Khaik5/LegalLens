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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContractDetailViewModel(private val repository: com.example.lagallens.domain.repository.ContractRepository) : ViewModel() {
    private var currentContractId: String? = null
    private var currentContract: EmploymentContract? = null
    private val _uiState = MutableStateFlow(ContractDetailUiState())
    val uiState = _uiState.asStateFlow()
    private val _uiEffect = MutableSharedFlow<ContractDetailUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun loadContract(contractId: String?) {
        if (contractId.isNullOrBlank() || currentContractId == contractId) return
        currentContractId = contractId
        viewModelScope.launch {
            try {
                val contract = repository.getContract(contractId)
                if (contract == null) {
                    emitEffect(ContractDetailUiEffect.NavigateBack)
                    return@launch
                }
                currentContract = contract
                val template = ContractListItem.allSamples.firstOrNull { it.id == contract.id } ?: ContractListItem.allSamples.first()
                _uiState.update {
                    ContractDetailUiState.from(template.copy(titleOverride = contract.title, documentId = contract.documentId, content = contract.content))
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                emitEffect(ContractDetailUiEffect.ShowMessage(R.string.auth_backend_not_configured))
            }
        }
    }

    fun onEvent(event: ContractDetailUiEvent) {
        when (event) {
            ContractDetailUiEvent.BackClicked -> emitEffect(ContractDetailUiEffect.NavigateBack)
            is ContractDetailUiEvent.AssistantActionClicked -> {
                val state = _uiState.value
                when (event.action) {
                    ContractAssistantAction.VIEW_DOCUMENT -> currentContractId?.let { emitEffect(ContractDetailUiEffect.NavigateToDocument(it)) }
                    ContractAssistantAction.COMPARE -> currentContractId?.let { emitEffect(ContractDetailUiEffect.NavigateToCompare(it)) }
                    ContractAssistantAction.OCR_TEXT -> emitEffect(ContractDetailUiEffect.NavigateToOcrText(
                        state.documentId, state.contractId, _uiState.value.renamedTitle ?: currentContract?.title.orEmpty(),
                        state.fileFormatRes, state.documentText
                    ))
                    ContractAssistantAction.ANALYZE -> emitEffect(ContractDetailUiEffect.NavigateToAnalysis(state.titleRes, state.contractTypeRes))
                    ContractAssistantAction.CHAT -> currentContractId?.let {
                        emitEffect(ContractDetailUiEffect.NavigateToChatbot(state.titleRes, state.contractTypeRes, it))
                    }
                    ContractAssistantAction.EXPORT -> emitEffect(ContractDetailUiEffect.ShowExportSheet)
                }
            }
            ContractDetailUiEvent.RenameClicked -> emitEffect(ContractDetailUiEffect.ShowRenameDialog)
            is ContractDetailUiEvent.RenameConfirmed -> {
                val id = currentContractId ?: return
                viewModelScope.launch {
                    if (repository.renameContract(id, event.name)) {
                        currentContract = currentContract?.copy(title = event.name.trim())
                        _uiState.update { it.copy(renamedTitle = event.name.trim()) }
                    } else emitEffect(ContractDetailUiEffect.ShowMessage(R.string.contract_rename_empty))
                }
            }
            ContractDetailUiEvent.DeleteClicked -> emitEffect(ContractDetailUiEffect.ShowDeleteConfirmation)
            ContractDetailUiEvent.DeleteConfirmed -> {
                val id = currentContractId ?: return
                viewModelScope.launch {
                    if (repository.deleteContract(id)) emitEffect(ContractDetailUiEffect.NavigateBack)
                    else emitEffect(ContractDetailUiEffect.ShowMessage(R.string.contract_detail_delete_unavailable))
                }
            }
        }
    }

    private fun emitEffect(effect: ContractDetailUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    class Factory(private val repository: com.example.lagallens.domain.repository.ContractRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ContractDetailViewModel::class.java))
            return ContractDetailViewModel(repository) as T
        }
    }
}
