package com.example.lagallens.presentation.feature.analysis.risk.findings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.risk.findings.contract.AnalysisRiskFindingsUiEffect
import com.example.lagallens.presentation.feature.analysis.risk.findings.contract.AnalysisRiskFindingsUiEvent
import com.example.lagallens.presentation.feature.analysis.risk.findings.contract.AnalysisRiskFindingsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.lagallens.presentation.feature.analysis.risk.findings.model.toRiskFinding

class AnalysisRiskFindingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisRiskFindingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<AnalysisRiskFindingsUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var scopedFindings = emptyList<com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskFinding>()

    private var resultId: String? = null

    fun clearResult() {
        resultId = null
        scopedFindings = emptyList()
        _uiState.value = AnalysisRiskFindingsUiState()
    }

    fun setResult(identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, detail: com.example.lagallens.domain.model.analysis.AnalysisDetail, isUnlocked: Boolean = false) {
        val count = com.example.lagallens.domain.model.analysis.AnalysisPreview.visibleCount(detail.findings.size, isUnlocked)
        val entries = detail.findings.take(count).map { it.toRiskFinding() }
        val filter = if (resultId == identity.resultId) _uiState.value.selectedFilter else
            com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskFilter.ALL
        resultId = identity.resultId
        scopedFindings = entries
        val filtered = entries.filter { filter.severity == null || it.severity == filter.severity }
        _uiState.update { it.copy(selectedFilter = filter, findings = filtered, lockedCount = detail.findings.size - count,
            status = if (filtered.isEmpty()) com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.EMPTY else com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.READY) }
    }

    fun onEvent(event: AnalysisRiskFindingsUiEvent) {
        when (event) {
            is AnalysisRiskFindingsUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisRiskFindingsUiEvent.BackClicked -> emitEffect(AnalysisRiskFindingsUiEffect.NavigateBack)
            is AnalysisRiskFindingsUiEvent.FilterClicked -> _uiState.update {
                val filtered = scopedFindings.filter { finding -> event.filter.severity == null || finding.severity == event.filter.severity }
                it.copy(selectedFilter = event.filter, findings = filtered,
                    status = if (filtered.isEmpty()) com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.EMPTY else com.example.lagallens.presentation.feature.analysis.SnapshotContentStatus.READY)
            }
            is AnalysisRiskFindingsUiEvent.FindingClicked -> {
                if (scopedFindings.any { it.id == event.findingId }) {
                    emitEffect(AnalysisRiskFindingsUiEffect.NavigateToRiskDetail(event.findingId))
                }
            }
        }
    }

    private fun emitEffect(effect: AnalysisRiskFindingsUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }

}
