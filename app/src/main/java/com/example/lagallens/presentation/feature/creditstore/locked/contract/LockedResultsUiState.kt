package com.example.lagallens.presentation.feature.creditstore.locked.contract

import androidx.annotation.StringRes
import com.example.lagallens.domain.model.analysis.LockedAnalysisResult

data class LockedResultsUiState(
    val isLoading: Boolean = true,
    val results: List<LockedAnalysisResult> = emptyList(),
    @param:StringRes val errorMessageRes: Int? = null
)
