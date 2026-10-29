package com.example.lagallens.presentation.feature.contracts.detail.export.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportStatus
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportUiEffect
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportUiEvent
import com.example.lagallens.presentation.feature.contracts.detail.export.contract.ContractExportUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContractExportViewModel : ViewModel() {
    private val state = MutableStateFlow(ContractExportUiState())
    val uiState = state.asStateFlow()
    private val effects = MutableSharedFlow<ContractExportUiEffect>()
    val uiEffect = effects.asSharedFlow()

    fun onEvent(event: ContractExportUiEvent) {
        when (event) {
            is ContractExportUiEvent.Opened -> if (!state.value.isInitialized) state.update {
                it.copy(title = event.title, metadata = event.metadata, isInitialized = true, status = if (event.showReadyPreview) ContractExportStatus.READY_PREVIEW else ContractExportStatus.OPTIONS)
            }
            ContractExportUiEvent.ExportClicked -> if (state.value.status == ContractExportStatus.OPTIONS) startDemoExport(true)
            ContractExportUiEvent.ShareClicked -> if (state.value.status == ContractExportStatus.OPTIONS) startDemoExport(false)
            ContractExportUiEvent.RetryClicked -> if (state.value.status == ContractExportStatus.ERROR) startDemoExport(true)
            ContractExportUiEvent.CancelClicked -> viewModelScope.launch { effects.emit(ContractExportUiEffect.Close) }
        }
    }

    private fun startDemoExport(showReadyPreview: Boolean) {
        state.update { it.copy(status = ContractExportStatus.LOADING) }
        viewModelScope.launch { delay(600); state.update { it.copy(status = if (showReadyPreview) ContractExportStatus.READY_PREVIEW else ContractExportStatus.ERROR) } }
    }
}
