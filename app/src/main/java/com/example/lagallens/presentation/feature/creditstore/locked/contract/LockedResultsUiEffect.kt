package com.example.lagallens.presentation.feature.creditstore.locked.contract

import com.example.lagallens.domain.model.analysis.LockedAnalysisResult

sealed interface LockedResultsUiEffect {
    data object NavigateBack : LockedResultsUiEffect
    data class OpenResult(val result: LockedAnalysisResult, val sessionKey: String) : LockedResultsUiEffect
}
