package com.example.lagallens.presentation.feature.analysis.risk.detail.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.feature.analysis.risk.detail.contract.AnalysisRiskDetailUiEffect
import com.example.lagallens.presentation.feature.analysis.risk.detail.contract.AnalysisRiskDetailUiEvent
import com.example.lagallens.presentation.feature.analysis.risk.detail.contract.AnalysisRiskDetailUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.lagallens.presentation.feature.analysis.risk.findings.contract.AnalysisRiskFindingsUiState
import kotlinx.coroutines.launch

class AnalysisRiskDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisRiskDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisRiskDetailUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun loadFinding(id: String) {
        val finding = AnalysisRiskFindingsUiState.allFindings.find { it.id == id } ?: return
        if (id == "penalty-cap") return
        _uiState.update { it.copy(
            isDetailedExample = false,
            severityRes = finding.severity.labelRes,
            severityBackgroundRes = finding.severity.badgeBackgroundRes,
            severityColorRes = finding.severity.labelColorRes,
            titleRes = finding.titleRes,
            attentionReasonRes = finding.consequenceRes,
            clauseReferenceRes = finding.clauseRes,
            clauseExcerptRes = finding.descriptionRes
        ) }
    }

    fun onEvent(event: AnalysisRiskDetailUiEvent) {
        when (event) {
            AnalysisRiskDetailUiEvent.BackClicked -> emitEffect(AnalysisRiskDetailUiEffect.NavigateBack)
            AnalysisRiskDetailUiEvent.LegalReferenceClicked -> emitEffect(
                AnalysisRiskDetailUiEffect.NavigateToCitation
            )
        }
    }

    private fun emitEffect(effect: AnalysisRiskDetailUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
