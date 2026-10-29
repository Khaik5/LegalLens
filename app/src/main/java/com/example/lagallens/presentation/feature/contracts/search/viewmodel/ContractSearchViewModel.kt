package com.example.lagallens.presentation.feature.contracts.search.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.presentation.feature.contracts.search.contract.ContractSearchResult
import com.example.lagallens.presentation.feature.contracts.search.contract.ContractSearchUiEffect
import com.example.lagallens.presentation.feature.contracts.search.contract.ContractSearchUiEvent
import com.example.lagallens.presentation.feature.contracts.search.contract.ContractSearchUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class ContractSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val initialQuery = application.getString(R.string.contract_search_default_query)
    private val _uiState = MutableStateFlow(
        ContractSearchUiState(
            query = initialQuery
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ContractSearchUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    private var contracts: List<EmploymentContract> = emptyList()

    init {
        viewModelScope.launch {
            try {
                (application as LegalLensApplication).contractRepository.observeContracts().collect { observed ->
                    contracts = observed
                    updateQuery(_uiState.value.query)
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                contracts = emptyList()
                updateQuery(_uiState.value.query)
            }
        }
    }

    fun onEvent(event: ContractSearchUiEvent) {
        when (event) {
            is ContractSearchUiEvent.QueryChanged -> updateQuery(event.query)
            is ContractSearchUiEvent.RecentSearchClicked -> updateQuery(event.query)
            ContractSearchUiEvent.ClearClicked -> updateQuery("")
            ContractSearchUiEvent.BackClicked -> navigateBack()
            is ContractSearchUiEvent.ResultClicked -> navigateToDetail(event.result.contractId)
        }
    }

    private fun updateQuery(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                results = activeResults().filter { item -> item.matches(query) }
            )
        }
    }

    private fun ContractSearchResult.matches(query: String): Boolean {
        val normalizedQuery = query.trim().lowercase()
        return normalizedQuery.isBlank() || titleOverride?.lowercase()?.contains(normalizedQuery) == true || searchKeywords.any { keyword ->
            keyword.contains(normalizedQuery) || normalizedQuery.contains(keyword)
        }
    }

    private fun activeResults(): List<ContractSearchResult> {
        val context = getApplication<Application>()
        return contracts.map { contract ->
            val template = ContractSearchResult.samples.firstOrNull { it.contractId == contract.id }
                ?: ContractSearchResult.samples.first()
            template.copy(
                contractId = contract.id,
                titleOverride = contract.title,
                subtitleOverride = context.getString(R.string.contract_demo_labor_subtitle),
                searchKeywords = listOf(contract.title, contract.content, "hợp đồng lao động", "người lao động")
            )
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _uiEffect.emit(ContractSearchUiEffect.NavigateBack)
        }
    }

    private fun navigateToDetail(contractId: String) {
        viewModelScope.launch {
            _uiEffect.emit(ContractSearchUiEffect.NavigateToDetail(contractId))
        }
    }
}
