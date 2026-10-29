package com.example.lagallens.presentation.feature.compare.select.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.presentation.feature.compare.select.contract.CompareContract
import com.example.lagallens.presentation.feature.compare.select.contract.CompareSelectUiEffect
import com.example.lagallens.presentation.feature.compare.select.contract.CompareSelectUiEvent
import com.example.lagallens.presentation.feature.compare.select.contract.CompareSelectUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CompareSelectViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as LegalLensApplication).contractRepository
    private val state = MutableStateFlow(CompareSelectUiState())
    val uiState = state.asStateFlow()
    private val effects = MutableSharedFlow<CompareSelectUiEffect>()
    val uiEffect = effects.asSharedFlow()
    private var allContracts: List<CompareContract> = emptyList()
    private var isNavigating = false

    init {
        viewModelScope.launch {
            try {
                repository.observeContracts().collect { contracts ->
                    allContracts = contracts.map(::toCompareContract)
                    val initialId = state.value.initialContractId?.takeIf { id -> allContracts.any { it.id == id } }
                    state.update { current ->
                        val selectedIds = current.selectedIds.filter { id -> allContracts.any { it.id == id } }
                            .ifEmpty { listOfNotNull(initialId) }
                        current.copy(contracts = allContracts.filter { it.title.contains(current.query, true) }, selectedIds = selectedIds, initialContractId = initialId, isInitialized = true)
                    }
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                state.update { it.copy(contracts = emptyList(), selectedIds = emptyList(), isInitialized = true) }
            }
        }
    }

    fun onEvent(event: CompareSelectUiEvent) {
        when (event) {
            is CompareSelectUiEvent.Initialize -> if (!state.value.isInitialized) state.update { it.copy(initialContractId = event.initialId) }
            is CompareSelectUiEvent.QueryChanged -> state.update { current -> current.copy(query = event.query, contracts = allContracts.filter { it.title.contains(event.query.trim(), true) }) }
            is CompareSelectUiEvent.ContractClicked -> state.update { current ->
                val selected = current.selectedIds
                val updated = when {
                    event.id in selected -> selected - event.id
                    selected.size < 2 && allContracts.any { it.id == event.id } -> selected + event.id
                    else -> selected
                }
                current.copy(selectedIds = updated)
            }
            CompareSelectUiEvent.BackClicked -> emit(CompareSelectUiEffect.NavigateBack)
            CompareSelectUiEvent.CompareClicked -> {
                val selected = state.value.selectedIds.mapNotNull { id -> allContracts.find { it.id == id } }
                if (selected.size == 2 && selected[0].id != selected[1].id && !isNavigating) {
                    isNavigating = true
                    emit(CompareSelectUiEffect.NavigateToResult(selected[0], selected[1]))
                }
            }
            CompareSelectUiEvent.ScreenResumed -> isNavigating = false
        }
    }

    private fun toCompareContract(contract: EmploymentContract): CompareContract {
        val context = getApplication<Application>()
        val title = if (contract.id in SAMPLE_IDS) {
            val template = com.example.lagallens.presentation.feature.contracts.contract.ContractListItem.allSamples.firstOrNull { it.id == contract.id }
            if (contract.title == template?.titleOverride) contract.title else contract.title
        } else contract.title
        return CompareContract(contract.id, title, context.getString(R.string.contract_demo_labor_subtitle))
    }

    private fun emit(effect: CompareSelectUiEffect) { viewModelScope.launch { effects.emit(effect) } }

    private companion object { val SAMPLE_IDS = setOf("office-lease", "collaborator", "equipment-purchase", "vehicle-lease") }
}
