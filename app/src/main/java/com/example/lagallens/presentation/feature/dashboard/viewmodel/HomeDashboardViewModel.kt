package com.example.lagallens.presentation.feature.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.dashboard.contract.HomeDashboardUiEffect
import com.example.lagallens.presentation.feature.dashboard.contract.HomeDashboardUiEvent
import com.example.lagallens.presentation.feature.dashboard.contract.HomeDashboardUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeDashboardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeDashboardUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<HomeDashboardUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: HomeDashboardUiEvent) {
        when (event) {
            HomeDashboardUiEvent.UploadClicked,
            HomeDashboardUiEvent.AnalyzeClicked -> emitEffect(HomeDashboardUiEffect.NavigateToUploadSource)

            HomeDashboardUiEvent.CameraClicked -> emitEffect(HomeDashboardUiEffect.NavigateToCamera)
            HomeDashboardUiEvent.CompareClicked -> emitEffect(HomeDashboardUiEffect.NavigateToCompare)
            HomeDashboardUiEvent.ViewAllContractsClicked -> emitEffect(HomeDashboardUiEffect.NavigateToContracts)
            is HomeDashboardUiEvent.ContractClicked -> emitEffect(
                HomeDashboardUiEffect.NavigateToContractDetail(event.contractId)
            )
        }
    }

    private fun showMessage(titleRes: Int) {
        emitEffect(HomeDashboardUiEffect.ShowMessage(titleRes))
    }

    private fun emitEffect(effect: HomeDashboardUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
