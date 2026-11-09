package com.example.lagallens.presentation.feature.analysis.summary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.summary.contract.AnalysisSummaryUiEffect
import com.example.lagallens.presentation.feature.analysis.summary.contract.AnalysisSummaryUiEvent
import com.example.lagallens.presentation.feature.analysis.summary.contract.AnalysisSummaryUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisSummaryViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisSummaryUiState(sections = emptyList()))
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<AnalysisSummaryUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun clearResult() { _uiState.value = AnalysisSummaryUiState() }

    fun setResult(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, detail: com.example.lagallens.domain.model.analysis.AnalysisDetail, isUnlocked: Boolean = false) {
        val entries = detail.summary.map { com.example.lagallens.presentation.feature.analysis.summary.contract.AnalysisSummarySection(it.id, it.title, it.description) }
        _uiState.update { it.copy(resultId = identity.resultId, sections = entries.take(com.example.lagallens.domain.model.analysis.AnalysisPreview.visibleCount(entries.size, isUnlocked)), lockedCount = entries.size - com.example.lagallens.domain.model.analysis.AnalysisPreview.visibleCount(entries.size, isUnlocked),
            status = if (entries.isEmpty()) com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.EMPTY else com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.READY) }
    }

    fun onEvent(event: AnalysisSummaryUiEvent) {
        when (event) {
            is AnalysisSummaryUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisSummaryUiEvent.BackClicked -> emitEffect(AnalysisSummaryUiEffect.NavigateBack)

            AnalysisSummaryUiEvent.ImportantInformationClicked -> emitEffect(
                AnalysisSummaryUiEffect.NavigateToKeyFacts(
                    documentName = uiState.value.documentName,
                    documentMetadata = uiState.value.documentMetadata
                )
            )
        }
    }

    private fun emitEffect(effect: AnalysisSummaryUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
