package com.example.lagallens.data.model.response

/** Only the parts of the backend comparison the app shows; unknown fields are ignored. */
data class ContractComparisonResponse(
    val first: ComparedContractResponse,
    val second: ComparedContractResponse,
    val summary: ComparisonSummaryResponse,
    val differences: List<TextDifferenceResponse>,
    val riskComparison: RiskComparisonResponse?,
    val overview: ComparisonOverviewResponse
)

data class ComparedContractResponse(
    val id: Long,
    val documentId: Long,
    val title: String,
    val fileName: String?,
    val mimeType: String?,
    val pageCount: Int?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val risk: String?
)

data class ComparisonSummaryResponse(
    val changedSections: Int,
    val addedSections: Int,
    val removedSections: Int,
    val unchangedSections: Int
)

/**
 * [kind] is PREAMBLE, ARTICLE, SECTION or CHAPTER; [matchedBy] is NUMBER or TITLE (a renumbered clause) and
 * [similarity] the word overlap 0..1, both null for an added or removed clause and on older backends.
 */
data class TextDifferenceResponse(
    val section: String,
    val firstText: String?,
    val secondText: String?,
    val changeType: String,
    val kind: String? = null,
    val matchedBy: String? = null,
    val similarity: Double? = null
)

/** [score] can be null for a result without a score; [matchesSource] is false when the analysis read another OCR revision. */
data class RiskSnapshotResponse(val score: Double?, val riskLevel: String, val matchesSource: Boolean? = null)

data class RiskComparisonResponse(val first: RiskSnapshotResponse, val second: RiskSnapshotResponse, val scoreDelta: Double?)

data class ComparisonOverviewResponse(
    val differences: List<FieldDifferenceResponse>,
    val first: ComparisonOverviewSideResponse? = null,
    val second: ComparisonOverviewSideResponse? = null
)

/** [analysisAccess] is PREVIEW when the key facts are only the free part of a locked analysis. */
data class ComparisonOverviewSideResponse(
    val keyFacts: List<ComparisonKeyFactResponse>? = null,
    val analysisAccess: String? = null
)

data class ComparisonKeyFactResponse(val label: String?, val value: String?, val reference: String?)

/** Values are strings, numbers, booleans or, for the parties, a list of objects on the wire. */
data class FieldDifferenceResponse(
    val field: String,
    val label: String,
    val firstValue: Any?,
    val secondValue: Any?,
    val changeType: String
)
