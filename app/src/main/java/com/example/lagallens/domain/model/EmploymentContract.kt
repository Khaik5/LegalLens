package com.example.lagallens.domain.model

enum class EmploymentContractStatus { COMPLETE, PROCESSING, DRAFT, PENDING_REVIEW }

enum class EmploymentContractRisk { LOW, MEDIUM, HIGH, CRITICAL, NOT_ANALYZED }

enum class EmploymentContractType { EMPLOYMENT, SERVICE, COMMERCIAL }

enum class EmploymentContractSource { UPLOADED, AI_GENERATED }

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
    val description: String? = null,
    /** The day the contract ends, when the analysis found one. */
    val endDate: java.time.LocalDate? = null,
    val source: EmploymentContractSource = EmploymentContractSource.UPLOADED,
    /** The linked draft version still needs the user's review; null when the backend does not say. */
    val reviewRequired: Boolean? = null
)

data class EmploymentContractComparison(
    val first: EmploymentContract,
    val second: EmploymentContract,
    val differences: List<ContractTextDifference>,
    /** Backend comparisons carry a summary, labelled field differences and a risk comparison; DEMO ones do not. */
    val summary: ContractComparisonSummary = ContractComparisonSummary(),
    val fieldDifferences: List<ContractFieldDifference> = emptyList(),
    val riskComparison: ContractRiskComparison? = null,
    val isSample: Boolean = false,
    /** Key facts of each contract's latest analysis; [isKeyFactsPreview] when a locked analysis only gave its free part. */
    val firstKeyFacts: List<ContractKeyFact> = emptyList(),
    val secondKeyFacts: List<ContractKeyFact> = emptyList(),
    val isKeyFactsPreview: Boolean = false
)

data class ContractKeyFact(val label: String, val value: String, val reference: String?)

enum class ContractChangeType { ADDED, REMOVED, MODIFIED, UNCHANGED }

/** PREAMBLE is the text before the first heading; its section title is only a display label. */
enum class ContractSectionKind { PREAMBLE, ARTICLE, SECTION, CHAPTER }

data class ContractTextDifference(
    val section: String,
    val firstText: String,
    val secondText: String,
    val changeType: ContractChangeType = ContractChangeType.MODIFIED,
    val kind: ContractSectionKind? = null,
    /** Word overlap 0..1 of both sides; null for an added or removed clause. */
    val similarity: Double? = null,
    /** The clause has another number on the other side and was paired by its title. */
    val isRenumbered: Boolean = false
)

data class ContractComparisonSummary(
    val changedSections: Int = 0,
    val addedSections: Int = 0,
    val removedSections: Int = 0,
    val unchangedSections: Int = 0
)

/** Null means the contract does not state the field. */
data class ContractFieldDifference(
    val field: String,
    val label: String,
    val firstValue: ContractFieldValue?,
    val secondValue: ContractFieldValue?,
    val changeType: ContractChangeType
)

/** [matchesSource] is false when the analysis read another version of the document than the one compared. */
data class ContractRiskScore(val score: Double?, val risk: EmploymentContractRisk, val matchesSource: Boolean? = null)

data class ContractRiskComparison(val first: ContractRiskScore, val second: ContractRiskScore, val scoreDelta: Double?)

/** A contract term as the backend states it: text (numbers keep their digits), a yes/no flag or the contract parties. */
sealed interface ContractFieldValue {
    data class Text(val value: String) : ContractFieldValue
    data class Flag(val isYes: Boolean) : ContractFieldValue
    data class Parties(val entries: List<ContractPartyValue>) : ContractFieldValue
}

/** [role] is the backend's role code, for example EMPLOYER, and null when the backend does not state one. */
data class ContractPartyValue(val name: String, val role: String?)
