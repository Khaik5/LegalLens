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
    val searchKeywords: List<String>
) {
    companion object {
        val samples = listOf(
            ContractSearchResult(
                contractId = "office-lease",
                titleRes = R.string.contract_office_lease,
                subtitleRes = R.string.contract_office_lease_search_subtitle,
                searchKeywords = listOf("hợp đồng thuê", "thuê văn phòng", "bitexco")
            ),
            ContractSearchResult(
                contractId = "vehicle-lease",
                titleRes = R.string.contract_vehicle_lease,
                subtitleRes = R.string.contract_vehicle_lease_search_subtitle,
                searchKeywords = listOf("hợp đồng thuê", "thuê xe", "đưa đón nhân viên")
            )
        )
    }
}
