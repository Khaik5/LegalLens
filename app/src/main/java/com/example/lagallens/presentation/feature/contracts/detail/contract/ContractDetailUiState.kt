package com.example.lagallens.presentation.feature.contracts.detail.contract

import androidx.annotation.DrawableRes
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.example.lagallens.R
import com.example.lagallens.core.network.DocumentFiles
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.presentation.feature.contracts.contract.ContractListItem
import com.example.lagallens.presentation.feature.contracts.contract.ContractRiskLevel

data class ContractDetailUiState(
    val contractId: String = "",
    val documentId: String = "",
    val documentText: String = "",
    val renamedTitle: String? = null,
    @StringRes val contractTypeRes: Int = R.string.contract_detail_type_employment,
    @StringRes val statusRes: Int = R.string.contract_draft_status,
    @StringRes val riskRes: Int = R.string.contract_not_analyzed_risk,
    val createdAtMillis: Long = 0,
    val updatedAtMillis: Long = 0,
    val pageCount: Int? = null,
    val fileFormat: String = "",
    @DrawableRes val statusBackgroundRes: Int = R.drawable.bg_contract_status_complete,
    @ColorRes val statusTextColorRes: Int = R.color.contract_complete,
    @DrawableRes val riskBackgroundRes: Int = R.drawable.bg_contract_detail_risk,
    @ColorRes val riskTextColorRes: Int = R.color.contract_complete,
    val assistantActions: List<ContractAssistantAction> = ContractAssistantAction.entries
) {
    companion object {
        fun from(item: ContractListItem, contract: EmploymentContract): ContractDetailUiState = ContractDetailUiState(
            contractId = item.id,
            documentId = item.documentId,
            documentText = item.content,
            renamedTitle = item.titleOverride,
            contractTypeRes = item.detailTypeRes,
            statusRes = item.statusRes,
            riskRes = item.riskRes,
            createdAtMillis = contract.createdAtMillis,
            updatedAtMillis = contract.updatedAtMillis,
            pageCount = contract.pageCount,
            fileFormat = contract.displayFileFormat(),
            statusBackgroundRes = item.statusBackgroundRes,
            statusTextColorRes = item.statusTextColorRes,
            riskBackgroundRes = when (item.riskLevel) {
                ContractRiskLevel.HIGH, ContractRiskLevel.CRITICAL -> R.drawable.bg_contract_detail_risk_high
                else -> R.drawable.bg_contract_detail_risk
            },
            riskTextColorRes = item.riskTextColorRes,
            // Word files cannot be recognized yet, so without saved text there is nothing to show.
            assistantActions = if (contract.isWordFile() && contract.content.isBlank()) {
                ContractAssistantAction.entries - ContractAssistantAction.OCR_TEXT
            } else ContractAssistantAction.entries
        )

        private fun EmploymentContract.isWordFile(): Boolean =
            mimeType == DocumentFiles.DOC || mimeType == DocumentFiles.DOCX

        private fun EmploymentContract.displayFileFormat(): String = when {
            mimeType.contains("pdf", ignoreCase = true) -> "PDF"
            mimeType.contains("word", ignoreCase = true) -> "Word"
            mimeType.startsWith("image/") -> mimeType.substringAfter('/').uppercase()
            else -> fileName.substringAfterLast('.', "").uppercase()
        }
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
