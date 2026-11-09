package com.example.lagallens.presentation.feature.contracts.contract

import com.example.lagallens.R
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractRisk
import com.example.lagallens.domain.model.EmploymentContractStatus
import com.example.lagallens.domain.model.EmploymentContractType

/** Every displayed value comes from the contract, so any stored or backend contract renders the same way. */
fun EmploymentContract.toContractListItem(subtitle: String): ContractListItem {
    val processingStatus = when (status) {
        EmploymentContractStatus.COMPLETE -> ContractProcessingStatus.COMPLETE
        EmploymentContractStatus.PROCESSING -> ContractProcessingStatus.PROCESSING
        EmploymentContractStatus.DRAFT -> ContractProcessingStatus.DRAFT
    }
    val riskLevel = when (risk) {
        EmploymentContractRisk.LOW -> ContractRiskLevel.LOW
        EmploymentContractRisk.MEDIUM -> ContractRiskLevel.MEDIUM
        EmploymentContractRisk.HIGH -> ContractRiskLevel.HIGH
        EmploymentContractRisk.CRITICAL -> ContractRiskLevel.CRITICAL
        EmploymentContractRisk.NOT_ANALYZED -> ContractRiskLevel.NOT_ANALYZED
    }
    val statusRes = when (processingStatus) {
        ContractProcessingStatus.COMPLETE -> R.string.contract_complete_status
        ContractProcessingStatus.PROCESSING -> R.string.contract_processing_status
        ContractProcessingStatus.DRAFT -> R.string.contract_draft_status
    }
    val riskRes = when (riskLevel) {
        ContractRiskLevel.LOW -> R.string.contract_low_risk
        ContractRiskLevel.MEDIUM -> R.string.contract_medium_risk
        ContractRiskLevel.HIGH, ContractRiskLevel.CRITICAL -> R.string.contract_high_risk
        ContractRiskLevel.NOT_ANALYZED -> R.string.contract_not_analyzed_risk
    }
    val (statusBackgroundRes, statusTextColorRes) = when (processingStatus) {
        ContractProcessingStatus.COMPLETE -> R.drawable.bg_contract_status_complete to R.color.contract_complete
        ContractProcessingStatus.PROCESSING -> R.drawable.bg_contract_status_processing to R.color.contract_pending
        ContractProcessingStatus.DRAFT -> R.drawable.bg_contract_status_draft to R.color.legal_lens_auth_accent
    }
    val isHighRisk = riskLevel == ContractRiskLevel.HIGH || riskLevel == ContractRiskLevel.CRITICAL
    val riskIconRes = when {
        isHighRisk -> R.drawable.ic_dashboard_risk_high
        riskLevel == ContractRiskLevel.NOT_ANALYZED -> R.drawable.ic_contract_risk_pending
        else -> R.drawable.ic_dashboard_risk_low
    }
    val riskTextColorRes = when {
        isHighRisk -> R.color.contract_risk_high
        riskLevel == ContractRiskLevel.NOT_ANALYZED -> R.color.legal_lens_secondary_text
        else -> R.color.contract_complete
    }
    val (contractType, detailTypeRes) = when (type) {
        EmploymentContractType.EMPLOYMENT -> ContractType.EMPLOYMENT to R.string.contract_detail_type_employment
        EmploymentContractType.SERVICE -> ContractType.SERVICE to R.string.contract_detail_type_service
        EmploymentContractType.COMMERCIAL -> ContractType.COMMERCIAL to R.string.contract_detail_type_commercial
    }
    return ContractListItem(
        id = id,
        statusRes = statusRes,
        riskRes = riskRes,
        detailTypeRes = detailTypeRes,
        contractType = contractType,
        processingStatus = processingStatus,
        riskLevel = riskLevel,
        statusBackgroundRes = statusBackgroundRes,
        statusTextColorRes = statusTextColorRes,
        contractIconBackgroundRes = if (isHighRisk) R.drawable.bg_contract_document_alert else R.drawable.bg_contract_document_normal,
        contractIconTintRes = if (isHighRisk) R.color.contract_risk_high else R.color.legal_lens_auth_accent,
        riskIconRes = riskIconRes,
        riskTextColorRes = riskTextColorRes,
        titleOverride = title,
        subtitleOverride = subtitle,
        documentId = documentId,
        content = content
    )
}
