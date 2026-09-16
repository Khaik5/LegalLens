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
            is HomeDashboardUiEvent.ShortcutClicked -> showMessage(event.titleRes)
            HomeDashboardUiEvent.ContractClicked,
            HomeDashboardUiEvent.ViewAllContractsClicked -> showMessage(R.string.menu_contracts)
        }
    }

    private fun showMessage(titleRes: Int) {
        viewModelScope.launch {
            _uiEffect.emit(HomeDashboardUiEffect.ShowMessage(titleRes))
        }
    }
}
