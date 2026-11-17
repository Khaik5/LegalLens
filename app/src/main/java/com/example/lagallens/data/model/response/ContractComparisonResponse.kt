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

data class TextDifferenceResponse(val section: String, val firstText: String?, val secondText: String?, val changeType: String)

data class RiskSnapshotResponse(val score: Double, val riskLevel: String)

data class RiskComparisonResponse(val first: RiskSnapshotResponse, val second: RiskSnapshotResponse, val scoreDelta: Double)

data class ComparisonOverviewResponse(val differences: List<FieldDifferenceResponse>)

/** Values are strings, numbers or booleans on the wire. */
data class FieldDifferenceResponse(
    val field: String,
    val label: String,
    val firstValue: Any?,
    val secondValue: Any?,
    val changeType: String
)
