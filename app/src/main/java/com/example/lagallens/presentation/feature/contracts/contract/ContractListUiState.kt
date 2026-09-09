package com.example.lagallens.presentation.feature.contracts.contract

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.lagallens.R

data class ContractListUiState(
    val query: String = "",
    val selectedFilter: ContractFilter = ContractFilter.ALL,
    val filterSelection: ContractFilterSelection = ContractFilterSelection(),
    val contracts: List<ContractListItem> = ContractListItem.samples
)

data class ContractFilterSelection(
    val contractTypes: Set<ContractType> = emptySet(),
    val status: ContractStatusFilter = ContractStatusFilter.ALL,
    val riskLevel: ContractRiskLevel? = null
)

enum class ContractType {
    SERVICE,
    EMPLOYMENT,
    COMMERCIAL
}

enum class ContractStatusFilter {
    ALL,
    COMPLETE,
    PROCESSING,
    DRAFT
}

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
    MEDIUM,
    HIGH,
    CRITICAL,
    NOT_ANALYZED
}

data class ContractListItem(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val statusRes: Int,
    @StringRes val riskRes: Int,
    @StringRes val detailTypeRes: Int,
    @StringRes val createdAtRes: Int,
    @StringRes val updatedAtRes: Int,
    @StringRes val pageCountRes: Int,
    @StringRes val fileFormatRes: Int,
    val contractType: ContractType,
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
        val allSamples = listOf(
            ContractListItem(
                id = "office-lease",
                titleRes = R.string.contract_office_lease,
                subtitleRes = R.string.contract_office_lease_subtitle,
                statusRes = R.string.contract_complete_status,
                riskRes = R.string.contract_low_risk,
                detailTypeRes = R.string.contract_detail_type_service,
                createdAtRes = R.string.contract_detail_office_created_at,
                updatedAtRes = R.string.contract_detail_office_updated_at,
                pageCountRes = R.string.contract_detail_office_page_count,
                fileFormatRes = R.string.contract_detail_office_file_format,
                contractType = ContractType.SERVICE,
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
                id = "collaborator",
                titleRes = R.string.contract_collaborator,
                subtitleRes = R.string.contract_collaborator_subtitle,
                statusRes = R.string.contract_processing_status,
                riskRes = R.string.contract_high_risk,
                detailTypeRes = R.string.contract_detail_type_employment,
                createdAtRes = R.string.contract_detail_collaborator_created_at,
                updatedAtRes = R.string.contract_detail_collaborator_updated_at,
                pageCountRes = R.string.contract_detail_collaborator_page_count,
                fileFormatRes = R.string.contract_detail_collaborator_file_format,
                contractType = ContractType.EMPLOYMENT,
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
                id = "equipment-purchase",
                titleRes = R.string.contract_equipment_purchase,
                subtitleRes = R.string.contract_equipment_purchase_subtitle,
                statusRes = R.string.contract_draft_status,
                riskRes = R.string.contract_not_analyzed_risk,
                detailTypeRes = R.string.contract_detail_type_commercial,
                createdAtRes = R.string.contract_detail_equipment_created_at,
                updatedAtRes = R.string.contract_detail_equipment_updated_at,
                pageCountRes = R.string.contract_detail_equipment_page_count,
                fileFormatRes = R.string.contract_detail_equipment_file_format,
                contractType = ContractType.COMMERCIAL,
                processingStatus = ContractProcessingStatus.DRAFT,
                riskLevel = ContractRiskLevel.NOT_ANALYZED,
                statusBackgroundRes = R.drawable.bg_contract_status_draft,
                statusTextColorRes = R.color.legal_lens_auth_accent,
                contractIconBackgroundRes = R.drawable.bg_contract_document_normal,
                contractIconTintRes = R.color.legal_lens_auth_accent,
                riskIconRes = R.drawable.ic_contract_risk_pending,
                riskTextColorRes = R.color.legal_lens_secondary_text
            ),
            ContractListItem(
                id = "vehicle-lease",
                titleRes = R.string.contract_vehicle_lease,
                subtitleRes = R.string.contract_vehicle_lease_search_subtitle,
                statusRes = R.string.contract_complete_status,
                riskRes = R.string.contract_low_risk,
                detailTypeRes = R.string.contract_detail_type_service,
                createdAtRes = R.string.contract_detail_vehicle_created_at,
                updatedAtRes = R.string.contract_detail_vehicle_updated_at,
                pageCountRes = R.string.contract_detail_vehicle_page_count,
                fileFormatRes = R.string.contract_detail_vehicle_file_format,
                contractType = ContractType.SERVICE,
                processingStatus = ContractProcessingStatus.COMPLETE,
                riskLevel = ContractRiskLevel.LOW,
                statusBackgroundRes = R.drawable.bg_contract_status_complete,
                statusTextColorRes = R.color.contract_complete,
                contractIconBackgroundRes = R.drawable.bg_contract_document_normal,
                contractIconTintRes = R.color.legal_lens_auth_accent,
                riskIconRes = R.drawable.ic_dashboard_risk_low,
                riskTextColorRes = R.color.contract_complete
            )
        )

        val samples = allSamples.take(3)
    }
}
