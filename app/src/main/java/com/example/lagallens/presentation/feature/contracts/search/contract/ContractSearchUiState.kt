package com.example.lagallens.presentation.feature.contracts.search.contract

import androidx.annotation.StringRes
import com.example.lagallens.R

data class ContractSearchUiState(
    val query: String = "",
    val results: List<ContractSearchResult> = emptyList()
)

data class ContractSearchResult(
    val contractId: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val searchKeywords: List<String>,
    val titleOverride: String? = null,
    val subtitleOverride: String? = null
) {
    companion object {
        val samples = listOf(
            ContractSearchResult(
                contractId = "office-lease",
                titleRes = R.string.contract_demo_labor_indefinite,
                subtitleRes = R.string.contract_demo_labor_subtitle,
                searchKeywords = listOf("hợp đồng lao động", "không xác định thời hạn", "nhân viên")
            ),
            ContractSearchResult(
                contractId = "vehicle-lease",
                titleRes = R.string.contract_demo_labor_fixed_term,
                subtitleRes = R.string.contract_demo_labor_subtitle,
                searchKeywords = listOf("hợp đồng lao động", "xác định thời hạn", "người lao động")
            )
        )
    }
}
