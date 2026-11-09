package com.example.lagallens.presentation.feature.contracts.detail.export.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportStatus
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportUiEffect
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportUiEvent
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContractExportViewModel : ViewModel() {
    private val state = MutableStateFlow(ContractExportUiState())
    val uiState = state.asStateFlow()
    private val effects = Channel<ContractExportUiEffect>(Channel.BUFFERED)
    val uiEffect = effects.receiveAsFlow()

    fun onEvent(event: ContractExportUiEvent) {
        when (event) {
            is ContractExportUiEvent.Opened -> if (!state.value.isInitialized) state.update {
                it.copy(title = event.title, metadata = event.metadata, isInitialized = true, status = if (event.showReadyPreview) ContractExportStatus.ERROR else ContractExportStatus.OPTIONS)
            }
            ContractExportUiEvent.ExportClicked -> if (state.value.status == ContractExportStatus.OPTIONS) showUnavailable()
            ContractExportUiEvent.ShareClicked -> if (state.value.status == ContractExportStatus.OPTIONS) showUnavailable()
            ContractExportUiEvent.RetryClicked -> if (state.value.status == ContractExportStatus.ERROR) showUnavailable()
            ContractExportUiEvent.CancelClicked -> viewModelScope.launch { effects.send(ContractExportUiEffect.Close) }
        }
    }

    private fun showUnavailable() {
        state.update { it.copy(status = ContractExportStatus.ERROR) }
    }
}
