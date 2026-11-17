package com.example.lagallens.domain.model

enum class EmploymentContractStatus { COMPLETE, PROCESSING, DRAFT }

enum class EmploymentContractRisk { LOW, MEDIUM, HIGH, CRITICAL, NOT_ANALYZED }

enum class EmploymentContractType { EMPLOYMENT, SERVICE, COMMERCIAL }

data class EmploymentContract(
    val id: String,
    val documentId: String,
    val title: String,
    val content: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val status: EmploymentContractStatus = EmploymentContractStatus.DRAFT,
    val risk: EmploymentContractRisk = EmploymentContractRisk.NOT_ANALYZED,
    val fileName: String = "",
    val fileUri: String? = null,
    val mimeType: String = "",
    val type: EmploymentContractType = EmploymentContractType.EMPLOYMENT,
    val pageCount: Int? = null,
    val generatedTitleKey: String? = null,
    val description: String? = null
)

data class EmploymentContractComparison(
    val first: EmploymentContract,
    val second: EmploymentContract,
    val differences: List<ContractTextDifference>,
    /** Backend comparisons carry a summary, labelled field differences and a risk comparison; DEMO ones do not. */
    val summary: ContractComparisonSummary = ContractComparisonSummary(),
    val fieldDifferences: List<ContractFieldDifference> = emptyList(),
    val riskComparison: ContractRiskComparison? = null,
    val isSample: Boolean = false
)

enum class ContractChangeType { ADDED, REMOVED, MODIFIED, UNCHANGED }

data class ContractTextDifference(
    val section: String,
    val firstText: String,
    val secondText: String,
    val changeType: ContractChangeType = ContractChangeType.MODIFIED
)

data class ContractComparisonSummary(
    val changedSections: Int = 0,
    val addedSections: Int = 0,
    val removedSections: Int = 0,
    val unchangedSections: Int = 0
)

/** [firstValue] and [secondValue] are display text; null means the contract does not state the field. */
data class ContractFieldDifference(
    val field: String,
    val label: String,
    val firstValue: String?,
    val secondValue: String?,
    val changeType: ContractChangeType
)

data class ContractRiskScore(val score: Double, val risk: EmploymentContractRisk)

data class ContractRiskComparison(val first: ContractRiskScore, val second: ContractRiskScore, val scoreDelta: Double)
