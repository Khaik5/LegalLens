package com.example.lagallens.presentation.feature.analysis.keyfacts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisKeyFactsUiEffect
import com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisKeyFactsUiEvent
import com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisKeyFactsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisKeyFactsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisKeyFactsUiState(facts = emptyList()))
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<AnalysisKeyFactsUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun clearResult() { _uiState.value = AnalysisKeyFactsUiState() }

    fun setResult(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, detail: com.example.lagallens.domain.model.analysis.AnalysisDetail, isUnlocked: Boolean = false) {
        val entries = detail.keyFacts.map { fact -> com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisKeyFact(fact.id, fact.label, fact.reference, fact.value, fact.description, fact.source?.let { com.example.lagallens.presentation.feature.analysis.keyfacts.contract.AnalysisClauseSource(it.reference, it.title, it.excerpt) }) }
        _uiState.update { it.copy(resultId = identity.resultId, facts = entries.take(com.example.lagallens.domain.model.analysis.AnalysisPreview.visibleCount(entries.size, isUnlocked)), lockedCount = entries.size - com.example.lagallens.domain.model.analysis.AnalysisPreview.visibleCount(entries.size, isUnlocked),
            status = if (entries.isEmpty()) com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.EMPTY else com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.READY) }
    }

    fun onEvent(event: AnalysisKeyFactsUiEvent) {
        when (event) {
            is AnalysisKeyFactsUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisKeyFactsUiEvent.BackClicked -> emitEffect(AnalysisKeyFactsUiEffect.NavigateBack)

            is AnalysisKeyFactsUiEvent.SourceClicked -> emitEffect(
                AnalysisKeyFactsUiEffect.ShowClauseSource(event.source)
            )
        }
    }

    private fun emitEffect(effect: AnalysisKeyFactsUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
