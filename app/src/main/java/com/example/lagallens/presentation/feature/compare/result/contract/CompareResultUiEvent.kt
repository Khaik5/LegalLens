package com.example.lagallens.presentation.feature.compare.result.contract

sealed interface CompareResultUiEvent {
    data class Opened(val firstId: String, val secondId: String, val showError: Boolean = false) : CompareResultUiEvent
    data class TabClicked(val tab: CompareResultTab) : CompareResultUiEvent
    data class SectionClicked(val id: String) : CompareResultUiEvent
    data object RetryClicked : CompareResultUiEvent
    data object BackClicked : CompareResultUiEvent
}
