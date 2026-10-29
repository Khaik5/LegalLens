package com.example.lagallens.presentation.feature.compare.result.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultStatus
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultUiEffect
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultUiEvent
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CompareResultViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as LegalLensApplication).contractRepository
    private val state = MutableStateFlow(CompareResultUiState())
    val uiState = state.asStateFlow()
    private val effects = MutableSharedFlow<CompareResultUiEffect>()
    val uiEffect = effects.asSharedFlow()
    private var firstId = ""
    private var secondId = ""

    fun onEvent(event: CompareResultUiEvent) {
        when (event) {
            is CompareResultUiEvent.Opened -> if (!state.value.isInitialized) {
                firstId = event.firstId
                secondId = event.secondId
                state.update { it.copy(isInitialized = true) }
                load(event.showError)
            }
            is CompareResultUiEvent.TabClicked -> if (state.value.status == CompareResultStatus.READY) state.update { it.copy(selectedTab = event.tab) }
            is CompareResultUiEvent.SectionClicked -> state.update { current ->
                current.copy(expandedSections = if (event.id in current.expandedSections) current.expandedSections - event.id else current.expandedSections + event.id)
            }
            CompareResultUiEvent.RetryClicked -> if (state.value.status == CompareResultStatus.ERROR) load(false)
            CompareResultUiEvent.BackClicked -> viewModelScope.launch { effects.emit(CompareResultUiEffect.NavigateBack) }
        }
    }

    private fun load(showError: Boolean) {
        state.update { it.copy(status = CompareResultStatus.LOADING, comparison = null) }
        viewModelScope.launch {
            try {
                if (showError) throw IllegalStateException("Demo error preview")
                val comparison = repository.compareContracts(firstId, secondId)
                if (comparison == null) {
                    state.update { it.copy(status = CompareResultStatus.NO_EXAMPLE) }
                } else {
                    state.update { it.copy(
                        status = CompareResultStatus.READY,
                        documentA = comparison.first.title,
                        documentB = comparison.second.title,
                        hasExample = true,
                        comparison = comparison
                    ) }
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (_: Exception) {
                state.update { it.copy(status = CompareResultStatus.ERROR) }
            }
        }
    }
}
