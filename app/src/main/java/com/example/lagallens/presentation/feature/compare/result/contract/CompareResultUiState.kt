package com.example.lagallens.presentation.feature.compare.result.contract

import androidx.annotation.StringRes
import com.example.lagallens.domain.model.EmploymentContractComparison

enum class CompareResultStatus { LOADING, READY, ERROR, NO_EXAMPLE }
enum class CompareResultTab { OVERVIEW, RISK, CLAUSES }

data class CompareResultUiState(
    val documentA: String = "",
    val documentB: String = "",
    val status: CompareResultStatus = CompareResultStatus.LOADING,
    val selectedTab: CompareResultTab = CompareResultTab.CLAUSES,
    val expandedSections: Set<String> = emptySet(),
    val isInitialized: Boolean = false,
    val hasExample: Boolean = false,
    val comparison: EmploymentContractComparison? = null,
    @param:StringRes val errorMessageRes: Int? = null
)
