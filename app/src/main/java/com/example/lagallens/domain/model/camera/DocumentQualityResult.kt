package com.example.lagallens.domain.model.camera

enum class QualityDecision {
    ACCEPTED,
    WARNING,
    REJECTED
}

data class DocumentQualityResult(
    val decision: QualityDecision,
    val issues: List<QualityIssue>
)
