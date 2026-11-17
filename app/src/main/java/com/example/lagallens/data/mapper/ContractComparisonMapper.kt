package com.example.lagallens.data.mapper

import com.example.lagallens.data.model.response.ComparedContractResponse
import com.example.lagallens.data.model.response.ContractComparisonResponse
import com.example.lagallens.domain.model.ContractChangeType
import com.example.lagallens.domain.model.ContractComparisonSummary
import com.example.lagallens.domain.model.ContractFieldDifference
import com.example.lagallens.domain.model.ContractRiskComparison
import com.example.lagallens.domain.model.ContractRiskScore
import com.example.lagallens.domain.model.ContractTextDifference
import com.example.lagallens.domain.model.EmploymentContract
import com.example.lagallens.domain.model.EmploymentContractComparison
import com.example.lagallens.domain.model.EmploymentContractRisk
import com.example.lagallens.domain.model.EmploymentContractStatus

internal fun ContractComparisonResponse.toComparison() = EmploymentContractComparison(
    first = first.toComparedContract(),
    second = second.toComparedContract(),
    differences = differences.map {
        ContractTextDifference(it.section, it.firstText.orEmpty(), it.secondText.orEmpty(), it.changeType.toChangeType())
    },
    summary = ContractComparisonSummary(
        changedSections = summary.changedSections,
        addedSections = summary.addedSections,
        removedSections = summary.removedSections,
        unchangedSections = summary.unchangedSections
    ),
    fieldDifferences = overview.differences.map {
        ContractFieldDifference(it.field, it.label, it.firstValue.toDisplayValue(), it.secondValue.toDisplayValue(), it.changeType.toChangeType())
    },
    riskComparison = riskComparison?.let {
        ContractRiskComparison(
            first = ContractRiskScore(it.first.score, it.first.riskLevel.toRisk()),
            second = ContractRiskScore(it.second.score, it.second.riskLevel.toRisk()),
            scoreDelta = it.scoreDelta
        )
    }
)

private fun ComparedContractResponse.toComparedContract() = EmploymentContract(
    id = id.toString(),
    documentId = documentId.toString(),
    title = title,
    content = "",
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    status = EmploymentContractStatus.COMPLETE,
    risk = risk.toRisk(),
    fileName = fileName.orEmpty(),
    mimeType = mimeType.orEmpty(),
    pageCount = pageCount
)

private fun String?.toChangeType() = when (this?.uppercase()) {
    "ADDED" -> ContractChangeType.ADDED
    "REMOVED" -> ContractChangeType.REMOVED
    "UNCHANGED" -> ContractChangeType.UNCHANGED
    else -> ContractChangeType.MODIFIED
}

private fun String?.toRisk() = when (this?.uppercase()) {
    "VERY_LOW", "LOW" -> EmploymentContractRisk.LOW
    "MEDIUM" -> EmploymentContractRisk.MEDIUM
    "HIGH" -> EmploymentContractRisk.HIGH
    "CRITICAL" -> EmploymentContractRisk.CRITICAL
    else -> EmploymentContractRisk.NOT_ANALYZED
}

/** Numbers arrive as JSON doubles, so a whole number is shown without ".0". */
private fun Any?.toDisplayValue(): String? = when (this) {
    null -> null
    is Double -> if (this % 1.0 == 0.0) toLong().toString() else toString()
    is Boolean -> if (this) "✓" else "✗"
    else -> toString().takeIf { it.isNotBlank() }
}
