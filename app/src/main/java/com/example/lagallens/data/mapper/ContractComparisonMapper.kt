package com.example.lagallens.data.mapper

import com.example.lagallens.data.model.response.ComparedContractResponse
import com.example.lagallens.data.model.response.ComparisonKeyFactResponse
import com.example.lagallens.domain.model.ContractKeyFact
import com.example.lagallens.domain.model.ContractSectionKind
import com.example.lagallens.data.model.response.ContractComparisonResponse
import com.example.lagallens.domain.model.ContractChangeType
import com.example.lagallens.domain.model.ContractComparisonSummary
import com.example.lagallens.domain.model.ContractFieldDifference
import com.example.lagallens.domain.model.ContractFieldValue
import com.example.lagallens.domain.model.ContractPartyValue
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
        ContractTextDifference(
            it.section, it.firstText.orEmpty(), it.secondText.orEmpty(), it.changeType.toChangeType(),
            kind = it.kind?.uppercase()?.let { kind -> ContractSectionKind.entries.firstOrNull { entry -> entry.name == kind } },
            similarity = it.similarity?.coerceIn(0.0, 1.0),
            isRenumbered = it.matchedBy.equals("TITLE", ignoreCase = true)
        )
    },
    firstKeyFacts = overview.first?.keyFacts.toKeyFacts(),
    secondKeyFacts = overview.second?.keyFacts.toKeyFacts(),
    isKeyFactsPreview = listOf(overview.first?.analysisAccess, overview.second?.analysisAccess).any { it.equals("PREVIEW", ignoreCase = true) },
    summary = ContractComparisonSummary(
        changedSections = summary.changedSections,
        addedSections = summary.addedSections,
        removedSections = summary.removedSections,
        unchangedSections = summary.unchangedSections
    ),
    fieldDifferences = overview.differences.map {
        ContractFieldDifference(it.field, it.label, it.firstValue.toFieldValue(), it.secondValue.toFieldValue(), it.changeType.toChangeType())
    },
    riskComparison = riskComparison?.let {
        ContractRiskComparison(
            first = ContractRiskScore(it.first.score, it.first.riskLevel.toRisk(), it.first.matchesSource),
            second = ContractRiskScore(it.second.score, it.second.riskLevel.toRisk(), it.second.matchesSource),
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

private fun List<ComparisonKeyFactResponse>?.toKeyFacts(): List<ContractKeyFact> = orEmpty().mapNotNull { fact ->
    val label = fact.label?.trim().takeUnless { it.isNullOrEmpty() } ?: return@mapNotNull null
    val value = fact.value?.trim().takeUnless { it.isNullOrEmpty() } ?: return@mapNotNull null
    ContractKeyFact(label, value, fact.reference?.trim()?.takeIf(String::isNotEmpty))
}

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

/** Numbers arrive as JSON doubles, so a whole number keeps no ".0". */
private fun Any?.toFieldValue(): ContractFieldValue? = when (this) {
    null -> null
    is Boolean -> ContractFieldValue.Flag(this)
    is Double -> ContractFieldValue.Text(if (this % 1.0 == 0.0) toLong().toString() else toString())
    is List<*> -> toParties()
    else -> toString().takeIf { it.isNotBlank() }?.let(ContractFieldValue::Text)
}

/** The backend sends the parties as a list of objects with a name and a role. */
private fun List<*>.toParties(): ContractFieldValue? = mapNotNull { entry ->
    val fields = entry as? Map<*, *> ?: return@mapNotNull null
    val name = fields["name"]?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
    ContractPartyValue(name, fields["role"]?.toString()?.takeIf { it.isNotBlank() })
}.takeIf { it.isNotEmpty() }?.let(ContractFieldValue::Parties)
