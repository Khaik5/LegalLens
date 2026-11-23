package com.example.lagallens.presentation.feature.compare.result.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.LegalLensApplication
import com.example.lagallens.R
import com.example.lagallens.domain.model.ContractComparisonBlocker
import com.example.lagallens.domain.model.ContractComparisonUnavailableException
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultStatus
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultUiEffect
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultUiEvent
import com.example.lagallens.presentation.feature.compare.result.contract.CompareResultUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CompareResultViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LegalLensApplication
    private val repository = app.contractRepository
    private val ownerSession = app.currentSessionKey()
    private val state = MutableStateFlow(CompareResultUiState())
    val uiState = state.asStateFlow()
    private val effects = Channel<CompareResultUiEffect>(Channel.BUFFERED)
    val uiEffect = effects.receiveAsFlow()
    private var firstId = ""
    private var secondId = ""

    init {
        viewModelScope.launch {
            app.observeSessionKeys().collect { session ->
                if (session != ownerSession) {
                    state.value = CompareResultUiState()
                    effects.send(CompareResultUiEffect.NavigateBack)
                }
            }
        }
    }

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
            CompareResultUiEvent.BackClicked -> viewModelScope.launch { effects.send(CompareResultUiEffect.NavigateBack) }
        }
    }

    private fun load(showError: Boolean) {
        state.update { it.copy(status = CompareResultStatus.LOADING, comparison = null, errorMessageRes = null) }
        viewModelScope.launch {
            try {
                if (showError) throw IllegalStateException("Demo error preview")
                val comparison = repository.compareContracts(firstId, secondId)
                if (app.currentSessionKey() != ownerSession) return@launch
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
            } catch (unavailable: ContractComparisonUnavailableException) {
                if (app.currentSessionKey() != ownerSession) return@launch
                state.update { it.copy(status = CompareResultStatus.ERROR, errorMessageRes = unavailable.blocker.messageRes()) }
            } catch (_: Exception) {
                state.update { it.copy(status = CompareResultStatus.ERROR) }
            }
        }
    }

    private fun ContractComparisonBlocker.messageRes() = when (this) {
        ContractComparisonBlocker.DOCUMENT_MISSING -> R.string.compare_error_document_missing
        ContractComparisonBlocker.OCR_NOT_READY -> R.string.compare_error_ocr_not_ready
        ContractComparisonBlocker.DOCUMENT_UNAVAILABLE -> R.string.compare_error_document_unavailable
        ContractComparisonBlocker.SAVED_DRAFT_TEXT_UNAVAILABLE -> R.string.compare_error_saved_draft_text_unavailable
        ContractComparisonBlocker.TOO_LARGE -> R.string.compare_error_too_large
        ContractComparisonBlocker.NOT_FOUND -> R.string.compare_error_not_found
        ContractComparisonBlocker.ACCESS_DENIED -> R.string.compare_error_access_denied
    }
}
