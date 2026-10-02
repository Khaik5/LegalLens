package com.example.lagallens.presentation.feature.analysis.risk.findings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.analysis.risk.findings.contract.AnalysisRiskFindingsUiEffect
import com.example.lagallens.presentation.feature.analysis.risk.findings.contract.AnalysisRiskFindingsUiEvent
import com.example.lagallens.presentation.feature.analysis.risk.findings.contract.AnalysisRiskFindingsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisRiskFindingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisRiskFindingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisRiskFindingsUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: AnalysisRiskFindingsUiEvent) {
        when (event) {
            AnalysisRiskFindingsUiEvent.BackClicked -> emitEffect(AnalysisRiskFindingsUiEffect.NavigateBack)
            is AnalysisRiskFindingsUiEvent.FilterClicked -> _uiState.update {
                it.copy(
                    selectedFilter = event.filter,
                    findings = AnalysisRiskFindingsUiState.allFindings.filter { finding ->
                        event.filter.severity == null || finding.severity == event.filter.severity
                    }
                )
            }
            is AnalysisRiskFindingsUiEvent.FindingClicked -> {
                val effect = if (event.findingId == PENALTY_CAP_FINDING_ID) {
                    AnalysisRiskFindingsUiEffect.NavigateToRiskDetail
                } else {
                    AnalysisRiskFindingsUiEffect.ShowMessage(
                        R.string.analysis_risk_findings_solution_unavailable
                    )
                }
                emitEffect(effect)
            }
        }
    }

    private fun emitEffect(effect: AnalysisRiskFindingsUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    private companion object {
        const val PENALTY_CAP_FINDING_ID = "penalty-cap"
    }
}
