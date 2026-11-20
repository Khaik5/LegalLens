package com.example.lagallens.domain.model.analysis

/** The latest finished analysis of a contract that the user has not unlocked yet. */
data class LockedAnalysisResult(
    val contractId: String,
    val contractTitle: String,
    val fileName: String,
    val resultId: String,
    val unlockCost: Int,
    val updatedAtMillis: Long
)
