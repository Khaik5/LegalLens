package com.example.lagallens.presentation.feature.compare.select.contract

sealed interface CompareSelectUiEffect {
    data object NavigateBack : CompareSelectUiEffect
    data class NavigateToResult(val first: CompareContract, val second: CompareContract) : CompareSelectUiEffect
}
