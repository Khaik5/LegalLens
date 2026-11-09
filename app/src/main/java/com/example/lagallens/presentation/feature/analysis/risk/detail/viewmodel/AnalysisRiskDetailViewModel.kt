package com.example.lagallens.presentation.feature.analysis.risk.detail.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.risk.detail.contract.AnalysisRiskDetailUiEffect
import com.example.lagallens.presentation.feature.analysis.risk.detail.contract.AnalysisRiskDetailUiEvent
import com.example.lagallens.presentation.feature.analysis.risk.detail.contract.AnalysisRiskDetailUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.lagallens.presentation.feature.analysis.risk.findings.contract.AnalysisRiskFindingsUiState
import kotlinx.coroutines.launch

class AnalysisRiskDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisRiskDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<AnalysisRiskDetailUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun clearResult() { _uiState.value = AnalysisRiskDetailUiState() }

    fun loadFinding(id: String, identity: com.example.lagallens.domain.model.analysis.AnalysisIdentity, detail: com.example.lagallens.domain.model.analysis.AnalysisDetail): Boolean {
        clearResult()
        if (id !in identity.findingIds) return false
        val finding = detail.findings.firstOrNull { it.id == id } ?: return false
        val severity = com.example.lagallens.presentation.feature.analysis.risk.findings.model.AnalysisRiskSeverity.valueOf(finding.severity.name)
        _uiState.update { it.copy(
            isDetailedExample = false,
            severityRes = severity.labelRes,
            severityBackgroundRes = severity.badgeBackgroundRes,
            severityColorRes = severity.labelColorRes,
            title = finding.title,
            attentionReason = finding.consequence,
            clauseReference = finding.clause,
            clauseExcerpt = finding.description,
            legalExplanation = finding.explanation,
            recommendation = finding.recommendation
        ) }
        return true
    }

    fun onEvent(event: AnalysisRiskDetailUiEvent) {
        when (event) {
            AnalysisRiskDetailUiEvent.BackClicked -> emitEffect(AnalysisRiskDetailUiEffect.NavigateBack)
            AnalysisRiskDetailUiEvent.LegalReferenceClicked -> emitEffect(
                AnalysisRiskDetailUiEffect.CitationUnavailable
            )
        }
    }

    private fun emitEffect(effect: AnalysisRiskDetailUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }
}
