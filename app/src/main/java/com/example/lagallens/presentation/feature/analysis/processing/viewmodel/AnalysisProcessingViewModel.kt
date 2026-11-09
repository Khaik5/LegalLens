package com.example.lagallens.presentation.feature.analysis.processing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.model.processing.AnalysisJobState
import com.example.lagallens.domain.repository.AnalysisIdentityRepository
import com.example.lagallens.domain.repository.AnalysisRepository
import com.example.lagallens.presentation.feature.analysis.processing.contract.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AnalysisProcessingViewModel(
    private val repository: AnalysisIdentityRepository,
    private val analysis: AnalysisRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisProcessingUiState())
    val uiState = _uiState.asStateFlow()
    private var started = false
    private var owner: String? = null
    private var generation = 0
    fun invalidateSession() { generation++; owner = null; _uiState.value = AnalysisProcessingUiState() }
    fun consumeCompletion(completion: AnalysisProcessingUiEffect) {
        _uiState.update { if (it.completion == completion) it.copy(completion = null) else it }
    }

    fun onEvent(event: AnalysisProcessingUiEvent) {
        when (event) {
            is AnalysisProcessingUiEvent.ScreenOpened -> {
                if (started) return
                if (event.sessionKey.isBlank() || repository.sessionKey() != event.sessionKey) return
                started = true
                owner = event.sessionKey
                val current = ++generation
                _uiState.update { it.copy(documentName = event.documentName, documentMetadata = event.documentMetadata) }
                viewModelScope.launch {
                    var resultId: String? = null
                    try {
                        analysis.process(event.operationId, event.contractId, event.documentId, event.sessionKey).collect { state ->
                            if (generation != current || repository.sessionKey() != event.sessionKey) return@collect
                            when (state) {
                                is AnalysisJobState.Running -> _uiState.update { it.withActiveStep(state.stage) }
                                is AnalysisJobState.Completed -> resultId = state.resultId
                                AnalysisJobState.Failed -> resultId = null
                            }
                        }
                    } catch (failure: CancellationException) { throw failure } catch (_: Exception) { resultId = null }
                    if (generation != current || owner != event.sessionKey || repository.sessionKey() != event.sessionKey) return@launch
                    val completion = resultId?.let { AnalysisProcessingUiEffect.NavigateToAnalysisResult(event.documentName, event.documentMetadata, it, event.sessionKey) }
                        ?: AnalysisProcessingUiEffect.NavigateToAnalysisFailure(event.documentName, event.documentMetadata)
                    _uiState.update { it.copy(completion = completion) }
                }
            }
        }
    }
    class Factory(private val repository: AnalysisIdentityRepository, private val analysis: AnalysisRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AnalysisProcessingViewModel(repository, analysis) as T
    }
}
