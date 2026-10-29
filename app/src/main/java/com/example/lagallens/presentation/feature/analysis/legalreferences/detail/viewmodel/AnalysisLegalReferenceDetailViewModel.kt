package com.example.lagallens.presentation.feature.analysis.legalreferences.detail.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.analysis.legalreferences.detail.contract.AnalysisLegalReferenceDetailUiEffect
import com.example.lagallens.presentation.feature.analysis.legalreferences.detail.contract.AnalysisLegalReferenceDetailUiEvent
import com.example.lagallens.presentation.feature.analysis.legalreferences.detail.contract.AnalysisLegalReferenceDetailUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalysisLegalReferenceDetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AnalysisLegalReferenceDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AnalysisLegalReferenceDetailUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: AnalysisLegalReferenceDetailUiEvent) {
        when (event) {
            is AnalysisLegalReferenceDetailUiEvent.ScreenOpened -> _uiState.update {
                val base = it.copy(
                    referenceId = event.referenceId,
                    documentName = event.documentName,
                    documentMetadata = event.documentMetadata
                )
                when (event.referenceId) {
                    "civil-code-2015-article-418" -> base.copy(
                        isDetailedExample = false,
                        titleRes = R.string.analysis_legal_references_civil_code_title,
                        articleRes = R.string.analysis_legal_references_civil_code_article,
                        effectiveDateRes = R.string.analysis_legal_references_civil_code_effective_date,
                        fullTextRes = R.string.analysis_legal_references_civil_code_description
                    )
                    "decree-21-2021-article-15" -> base.copy(
                        isDetailedExample = false,
                        titleRes = R.string.analysis_legal_references_decree_title,
                        articleRes = R.string.analysis_legal_references_decree_article,
                        effectiveDateRes = R.string.analysis_legal_references_decree_effective_date,
                        fullTextRes = R.string.analysis_legal_references_decree_description
                    )
                    else -> base
                }
            }

            AnalysisLegalReferenceDetailUiEvent.BackClicked -> emitEffect(
                AnalysisLegalReferenceDetailUiEffect.NavigateBack
            )

            AnalysisLegalReferenceDetailUiEvent.RelatedRiskClicked -> emitEffect(
                AnalysisLegalReferenceDetailUiEffect.NavigateToRelatedRisk
            )
        }
    }

    private fun emitEffect(effect: AnalysisLegalReferenceDetailUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }
}
