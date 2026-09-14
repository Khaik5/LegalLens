package com.example.lagallens.presentation.feature.contracts.detail.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiEffect
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiEvent
import com.example.lagallens.presentation.feature.contracts.detail.contract.ContractDetailUiState
import com.example.lagallens.presentation.feature.contracts.contract.ContractListItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContractDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ContractDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ContractDetailUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun loadContract(contractId: String?) {
        val contract = ContractListItem.allSamples.find { it.id == contractId }
            ?: ContractListItem.samples.first()
        _uiState.update { ContractDetailUiState.from(contract) }
    }

    fun onEvent(event: ContractDetailUiEvent) {
        when (event) {
            ContractDetailUiEvent.BackClicked -> emitEffect(ContractDetailUiEffect.NavigateBack)
            is ContractDetailUiEvent.AssistantActionClicked -> {
                emitEffect(ContractDetailUiEffect.ShowMessage(R.string.contract_detail_feature_unavailable))
            }
            ContractDetailUiEvent.RenameClicked -> {
                emitEffect(ContractDetailUiEffect.ShowMessage(R.string.contract_detail_rename_unavailable))
            }
            ContractDetailUiEvent.DeleteClicked -> emitEffect(ContractDetailUiEffect.ShowDeleteConfirmation)
            ContractDetailUiEvent.DeleteConfirmed -> {
                emitEffect(ContractDetailUiEffect.ShowMessage(R.string.contract_detail_delete_unavailable))
            }
        }
    }

    private fun emitEffect(effect: ContractDetailUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
