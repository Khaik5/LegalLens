package com.example.lagallens.presentation.feature.creditstore.locked.contract

import com.example.lagallens.domain.model.analysis.LockedAnalysisResult

sealed interface LockedResultsUiEvent {
    data object ScreenResumed : LockedResultsUiEvent
    data object RetryClicked : LockedResultsUiEvent
    data object BackClicked : LockedResultsUiEvent
    data class ResultClicked(val result: LockedAnalysisResult) : LockedResultsUiEvent
}
