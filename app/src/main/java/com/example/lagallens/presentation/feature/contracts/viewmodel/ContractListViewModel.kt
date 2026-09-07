package com.example.lagallens.presentation.feature.contracts.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.contracts.contract.ContractFilter
import com.example.lagallens.presentation.feature.contracts.contract.ContractListItem
import com.example.lagallens.presentation.feature.contracts.contract.ContractListUiEffect
import com.example.lagallens.presentation.feature.contracts.contract.ContractListUiEvent
import com.example.lagallens.presentation.feature.contracts.contract.ContractListUiState
import com.example.lagallens.presentation.feature.contracts.contract.ContractProcessingStatus
import com.example.lagallens.presentation.feature.contracts.contract.ContractRiskLevel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContractListViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ContractListUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ContractListUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: ContractListUiEvent) {
        when (event) {
            is ContractListUiEvent.QueryChanged -> updateContracts(query = event.query)
            is ContractListUiEvent.FilterSelected -> updateContracts(filter = event.filter)
            is ContractListUiEvent.ContractClicked -> showDetailUnavailable()
        }
    }

    private fun updateContracts(
        query: String = _uiState.value.query,
        filter: ContractFilter = _uiState.value.selectedFilter
    ) {
        _uiState.update {
            it.copy(
                query = query,
                selectedFilter = filter,
                contracts = ContractListItem.samples.filter { item ->
                    item.matches(filter) && item.matches(query)
                }
            )
        }
    }

    private fun ContractListItem.matches(filter: ContractFilter): Boolean = when (filter) {
        ContractFilter.ALL -> true
        ContractFilter.ANALYZED -> processingStatus == ContractProcessingStatus.COMPLETE
        ContractFilter.PROCESSING -> processingStatus == ContractProcessingStatus.PROCESSING
        ContractFilter.HIGH_RISK -> riskLevel == ContractRiskLevel.HIGH
    }

    private fun ContractListItem.matches(query: String): Boolean {
        val normalizedQuery = query.trim().lowercase()
        return normalizedQuery.isBlank() || getApplication<Application>()
            .getString(titleRes)
            .lowercase()
            .contains(normalizedQuery)
    }

    private fun showDetailUnavailable() {
        viewModelScope.launch {
            _uiEffect.emit(ContractListUiEffect.ShowMessage(R.string.contract_detail_unavailable))
        }
    }
}
