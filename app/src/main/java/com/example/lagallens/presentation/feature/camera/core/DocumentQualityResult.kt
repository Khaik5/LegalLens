package com.example.lagallens.presentation.feature.camera.core

enum class QualityDecision {
    ACCEPTED,
    WARNING,
    REJECTED
}

data class DocumentQualityResult(
    val decision: QualityDecision,
    val issues: List<QualityIssue>
)
