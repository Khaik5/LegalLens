package com.example.lagallens.presentation.feature.contracts.search.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.presentation.feature.contracts.contract.labelRes
import com.example.lagallens.presentation.feature.contracts.search.contract.ContractSearchResult
import com.example.lagallens.presentation.feature.contracts.search.contract.ContractSearchUiEffect
import com.example.lagallens.presentation.feature.contracts.search.contract.ContractSearchUiEvent
import com.example.lagallens.presentation.feature.contracts.search.contract.ContractSearchUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

internal fun ContractSearchResult.matchesQuery(query: String): Boolean {
    val normalizedQuery = query.trim().lowercase()
    return normalizedQuery.isBlank() || titleOverride?.lowercase()?.contains(normalizedQuery) == true ||
        searchKeywords.any { it.lowercase().contains(normalizedQuery) }
}

/** Matches only what belongs to this contract: its title, text, file name and type. */
internal fun EmploymentContract.toSearchResult(typeLabel: String) = ContractSearchResult(
    contractId = id,
    titleRes = type.labelRes(),
    subtitleRes = type.labelRes(),
    searchKeywords = listOf(title, content, fileName, typeLabel).filter(String::isNotBlank),
    titleOverride = title,
    subtitleOverride = typeLabel
)

class ContractSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(ContractSearchUiState(query = ""))
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<ContractSearchUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

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
            ContractSearchUiEvent.ClearClicked -> updateQuery("")
            ContractSearchUiEvent.BackClicked -> navigateBack()
            is ContractSearchUiEvent.ResultClicked -> navigateToDetail(event.result.contractId)
        }
    }

    private fun updateQuery(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                results = activeResults().filter { item -> item.matchesQuery(query) }
            )
        }
    }

    private fun activeResults(): List<ContractSearchResult> {
        val context = getApplication<Application>()
        return contracts.map { contract -> contract.toSearchResult(context.getString(contract.type.labelRes())) }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _uiEffect.send(ContractSearchUiEffect.NavigateBack)
        }
    }

    private fun navigateToDetail(contractId: String) {
        viewModelScope.launch {
            _uiEffect.send(ContractSearchUiEffect.NavigateToDetail(contractId))
        }
    }
}
