package com.example.lagallens.presentation.feature.contracts.detail.contract

import androidx.annotation.DrawableRes
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.contracts.contract.ContractListItem
import com.example.lagallens.presentation.feature.contracts.contract.ContractRiskLevel

data class ContractDetailUiState(
    @StringRes val titleRes: Int = R.string.contract_office_lease,
    @StringRes val contractTypeRes: Int = R.string.contract_detail_type_service,
    @StringRes val statusRes: Int = R.string.contract_complete_status,
    @StringRes val riskRes: Int = R.string.contract_low_risk,
    @StringRes val createdAtRes: Int = R.string.contract_detail_office_created_at,
    @StringRes val updatedAtRes: Int = R.string.contract_detail_office_updated_at,
    @StringRes val pageCountRes: Int = R.string.contract_detail_office_page_count,
    @StringRes val fileFormatRes: Int = R.string.contract_detail_office_file_format,
    @DrawableRes val statusBackgroundRes: Int = R.drawable.bg_contract_status_complete,
    @ColorRes val statusTextColorRes: Int = R.color.contract_complete,
    @DrawableRes val riskBackgroundRes: Int = R.drawable.bg_contract_detail_risk,
    @ColorRes val riskTextColorRes: Int = R.color.contract_complete,
    val assistantActions: List<ContractAssistantAction> = ContractAssistantAction.entries
) {
    companion object {
        fun from(contract: ContractListItem): ContractDetailUiState = ContractDetailUiState(
            titleRes = contract.titleRes,
            contractTypeRes = contract.detailTypeRes,
            statusRes = contract.statusRes,
            riskRes = contract.riskRes,
            createdAtRes = contract.createdAtRes,
            updatedAtRes = contract.updatedAtRes,
            pageCountRes = contract.pageCountRes,
            fileFormatRes = contract.fileFormatRes,
            statusBackgroundRes = contract.statusBackgroundRes,
            statusTextColorRes = contract.statusTextColorRes,
            riskBackgroundRes = when (contract.riskLevel) {
                ContractRiskLevel.HIGH, ContractRiskLevel.CRITICAL -> R.drawable.bg_contract_detail_risk_high
                else -> R.drawable.bg_contract_detail_risk
            },
            riskTextColorRes = contract.riskTextColorRes
        )
    }
}

enum class ContractAssistantAction(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int
) {
    VIEW_DOCUMENT(R.string.contract_detail_view_document, R.drawable.ic_contract_search_document),
    OCR_TEXT(R.string.contract_detail_ocr_text, R.drawable.ic_contract_detail_scan),
    ANALYZE(R.string.contract_detail_analyze, R.drawable.ic_contract_detail_brain),
    CHAT(R.string.contract_detail_chat, R.drawable.ic_contract_detail_chat),
    COMPARE(R.string.contract_detail_compare, R.drawable.ic_contract_detail_compare),
    EXPORT(R.string.contract_detail_export, R.drawable.ic_contract_detail_download)
}
