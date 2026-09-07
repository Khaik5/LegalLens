package com.example.lagallens.presentation.feature.contracts.contract

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.lagallens.R

data class ContractListUiState(
    val query: String = "",
    val selectedFilter: ContractFilter = ContractFilter.ALL,
    val contracts: List<ContractListItem> = ContractListItem.samples
)

enum class ContractFilter(@StringRes val titleRes: Int) {
    ALL(R.string.contract_filter_all),
    ANALYZED(R.string.contract_filter_analyzed),
    PROCESSING(R.string.contract_filter_processing),
    HIGH_RISK(R.string.contract_filter_high_risk)
}

enum class ContractProcessingStatus {
    COMPLETE,
    PROCESSING,
    DRAFT
}

enum class ContractRiskLevel {
    LOW,
    HIGH,
    NOT_ANALYZED
}

data class ContractListItem(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val statusRes: Int,
    @StringRes val riskRes: Int,
    val processingStatus: ContractProcessingStatus,
    val riskLevel: ContractRiskLevel,
    @DrawableRes val statusBackgroundRes: Int,
    @ColorRes val statusTextColorRes: Int,
    @DrawableRes val contractIconBackgroundRes: Int,
    @ColorRes val contractIconTintRes: Int,
    @DrawableRes val riskIconRes: Int,
    @ColorRes val riskTextColorRes: Int
) {
    companion object {
        val samples = listOf(
            ContractListItem(
                titleRes = R.string.contract_office_lease,
                subtitleRes = R.string.contract_office_lease_subtitle,
                statusRes = R.string.contract_complete_status,
                riskRes = R.string.contract_low_risk,
                processingStatus = ContractProcessingStatus.COMPLETE,
                riskLevel = ContractRiskLevel.LOW,
                statusBackgroundRes = R.drawable.bg_contract_status_complete,
                statusTextColorRes = R.color.contract_complete,
                contractIconBackgroundRes = R.drawable.bg_contract_document_normal,
                contractIconTintRes = R.color.legal_lens_auth_accent,
                riskIconRes = R.drawable.ic_dashboard_risk_low,
                riskTextColorRes = R.color.contract_complete
            ),
            ContractListItem(
                titleRes = R.string.contract_collaborator,
                subtitleRes = R.string.contract_collaborator_subtitle,
                statusRes = R.string.contract_processing_status,
                riskRes = R.string.contract_high_risk,
                processingStatus = ContractProcessingStatus.PROCESSING,
                riskLevel = ContractRiskLevel.HIGH,
                statusBackgroundRes = R.drawable.bg_contract_status_processing,
                statusTextColorRes = R.color.contract_pending,
                contractIconBackgroundRes = R.drawable.bg_contract_document_alert,
                contractIconTintRes = R.color.contract_risk_high,
                riskIconRes = R.drawable.ic_dashboard_risk_high,
                riskTextColorRes = R.color.contract_risk_high
            ),
            ContractListItem(
                titleRes = R.string.contract_equipment_purchase,
                subtitleRes = R.string.contract_equipment_purchase_subtitle,
                statusRes = R.string.contract_draft_status,
                riskRes = R.string.contract_not_analyzed_risk,
                processingStatus = ContractProcessingStatus.DRAFT,
                riskLevel = ContractRiskLevel.NOT_ANALYZED,
                statusBackgroundRes = R.drawable.bg_contract_status_draft,
                statusTextColorRes = R.color.legal_lens_auth_accent,
                contractIconBackgroundRes = R.drawable.bg_contract_document_normal,
                contractIconTintRes = R.color.legal_lens_auth_accent,
                riskIconRes = R.drawable.ic_contract_risk_pending,
                riskTextColorRes = R.color.legal_lens_secondary_text
            )
        )
    }
}
