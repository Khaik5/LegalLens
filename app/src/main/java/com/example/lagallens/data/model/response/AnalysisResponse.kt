package com.example.lagallens.data.model.response

data class AnalysisJobCreatedResponse(val jobId: String, val status: String)

/** [status] is one of PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED. */
data class AnalysisJobResponse(
    val jobId: String,
    val status: String,
    val progress: Int?,
    val errorCode: String?,
    val retryable: Boolean?
)

data class AnalysisLatestResponse(
    val jobId: String,
    val contractId: Long?,
    val documentId: Long?,
    val status: String,
    val isUnlocked: Boolean?
)

data class AnalysisUnlockStatusResponse(val isUnlocked: Boolean, val costCredits: Int, val balance: Int)

data class AnalysisUnlockResponse(val isUnlocked: Boolean, val balanceAfter: Int?)

/** Preview results hold only the first part of each list; [access] tells how many items stay hidden. */
data class AnalysisResultResponse(
    val jobId: String,
    val contractId: Long?,
    val documentId: Long?,
    val overview: AnalysisOverviewResponse?,
    val summarySections: List<AnalysisSummaryResponse>?,
    val keyFacts: List<AnalysisKeyFactResponse>?,
    val parties: List<AnalysisPartyResponse>?,
    val obligations: List<AnalysisObligationResponse>?,
    val findings: List<AnalysisFindingResponse>?,
    val citations: List<AnalysisCitationResponse>?,
    val access: AnalysisAccessResponse?
)

data class AnalysisOverviewResponse(
    val score: Int?,
    val overallRiskLevel: String?,
    val keyFactsCount: Int?,
    val riskCount: Int?,
    val referenceCount: Int?
)

data class AnalysisAccessResponse(val isUnlocked: Boolean?, val view: String?, val lockedCounts: Map<String, Int>?)

data class AnalysisSummaryResponse(val id: String?, val title: String?, val description: String?)

data class AnalysisSourceResponse(val reference: String?, val title: String?, val excerpt: String?, val page: Int?)

data class AnalysisKeyFactResponse(
    val id: String?,
    val label: String?,
    val value: String?,
    val reference: String?,
    val description: String?,
    val source: AnalysisSourceResponse?
)

data class AnalysisPartyResponse(val id: String?, val title: String?, val role: String?, val fields: List<AnalysisPartyFieldResponse>?)

data class AnalysisPartyFieldResponse(val id: String?, val label: String?, val value: String?)

/** [kind] is RIGHT or DUTY; [party] is EMPLOYER, EMPLOYEE or OTHER. */
data class AnalysisObligationResponse(
    val id: String?,
    val party: String?,
    val kind: String?,
    val title: String?,
    val clause: String?,
    val description: String?,
    val source: AnalysisSourceResponse?
)

data class AnalysisFindingResponse(
    val id: String?,
    val severity: String?,
    val clause: String?,
    val title: String?,
    val description: String?,
    val consequence: String?,
    val explanation: String?,
    val recommendation: String?
)

data class AnalysisCitationResponse(
    val id: String?,
    val legalArticleId: Long?,
    val documentTitle: String?,
    val articleNumber: String?,
    val title: String?,
    val topic: String?,
    val excerpt: String?,
    val effectiveDate: String?,
    val validityStatus: String?
)
