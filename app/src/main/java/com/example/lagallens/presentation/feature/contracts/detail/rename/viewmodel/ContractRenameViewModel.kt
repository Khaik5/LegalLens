package com.example.lagallens.presentation.feature.contracts.detail.rename.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.contracts.detail.rename.contract.ContractRenameUiEffect
import com.example.lagallens.presentation.feature.contracts.detail.rename.contract.ContractRenameUiEvent
import com.example.lagallens.presentation.feature.contracts.detail.rename.contract.ContractRenameUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContractRenameViewModel : ViewModel() {
    private val state = MutableStateFlow(ContractRenameUiState())
    val uiState = state.asStateFlow()
    private val effects = Channel<ContractRenameUiEffect>(Channel.BUFFERED)
    val uiEffect = effects.receiveAsFlow()

    fun onEvent(event: ContractRenameUiEvent) {
        when (event) {
            is ContractRenameUiEvent.Opened -> if (!state.value.isInitialized) state.update { it.copy(name = event.name, isInitialized = true) }
            is ContractRenameUiEvent.NameChanged -> if (!state.value.isSaving) state.update { it.copy(name = event.name, isTouched = true) }
            ContractRenameUiEvent.CancelClicked -> if (!state.value.isSaving) emit(ContractRenameUiEffect.Close)
            ContractRenameUiEvent.SaveClicked -> if (state.value.canSave) {
                val name = state.value.name.trim()
                state.update { it.copy(isSaving = true) }
                viewModelScope.launch { delay(500); effects.send(ContractRenameUiEffect.Saved(name)) }
            }
        }
    }

    private fun emit(effect: ContractRenameUiEffect) { viewModelScope.launch { effects.send(effect) } }
}
