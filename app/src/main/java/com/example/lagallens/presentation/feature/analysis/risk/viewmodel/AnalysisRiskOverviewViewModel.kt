package com.example.lagallens.presentation.feature.analysis.risk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.risk.contract.AnalysisRiskOverviewUiEffect
import com.example.lagallens.presentation.feature.analysis.risk.contract.AnalysisRiskOverviewUiEvent
import com.example.lagallens.presentation.feature.analysis.risk.contract.AnalysisRiskOverviewUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskSeverity

class AnalysisRiskOverviewViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisRiskOverviewUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<AnalysisRiskOverviewUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun clearResult() { _uiState.value = AnalysisRiskOverviewUiState() }

    fun setResult(
        identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity,
        detail: com.example.lagallens.domain.model.analysis.AnalysisDetail,
        isUnlocked: Boolean = false
    ) {
        // Locked findings are empty placeholders (see AnalysisResultMapper). They are not real findings, so they are
        // neither counted nor used to work out the highest level; the server level covers every finding. The severity
        // counts cover the same share as the findings list shows, and the total says how many findings exist.
        val visibleCount = com.example.lagallens.domain.model.analysis.AnalysisPreview.visibleCount(detail.findings.size, isUnlocked)
        val findings = detail.findings.take(visibleCount).filterNot { it.id.startsWith("locked-finding-") }
            .map { AnalysisRiskSeverity.valueOf(it.severity.name) }
        val serverSeverity = identity.overview?.severity?.let { AnalysisRiskSeverity.valueOf(it.name) }
        _uiState.update { it.copy(score = identity.overview?.score?.takeIf { score -> score in 0..100 },
            highestSeverity = serverSeverity ?: AnalysisRiskSeverity.entries.firstOrNull { level -> level in findings },
            highRiskCount = findings.count { it == AnalysisRiskSeverity.HIGH },
            mediumRiskCount = findings.count { it == AnalysisRiskSeverity.MEDIUM },
            lowRiskCount = findings.count { it == AnalysisRiskSeverity.LOW },
            totalRiskCount = detail.findings.size,
            lockedRiskCount = detail.findings.size - visibleCount) }
    }

    fun onEvent(event: AnalysisRiskOverviewUiEvent) {
        when (event) {
            is AnalysisRiskOverviewUiEvent.ScreenOpened -> _uiState.update {
                it.copy(
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
            }

            AnalysisRiskOverviewUiEvent.BackClicked -> emitEffect(AnalysisRiskOverviewUiEffect.NavigateBack)

            AnalysisRiskOverviewUiEvent.ViewDetailsClicked -> emitEffect(
                AnalysisRiskOverviewUiEffect.NavigateToFindings(
                    documentName = uiState.value.documentName,
                    documentMetadata = uiState.value.documentMetadata
                )
            )
        }
    }

    private fun emitEffect(effect: AnalysisRiskOverviewUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
