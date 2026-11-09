package com.example.lagallens.domain.model.processing

/** Progress of a long-running document job, reported by the data source. */
sealed interface ProcessingState {
    /** [progressPercent] and [stage] are optional because not every job reports both. */
    data class Running(val progressPercent: Int? = null, val stage: Int? = null) : ProcessingState
    data object Completed : ProcessingState
    data class Failed(val isRetryable: Boolean = true) : ProcessingState
}

sealed interface AnalysisJobState {
    data class Running(val stage: Int) : AnalysisJobState
    data class Completed(val resultId: String) : AnalysisJobState
    data object Failed : AnalysisJobState
}

/** [sourceUris] holds one file or every scanned page of the document. */
data class DocumentUploadRequest(
    val documentId: String,
    val displayName: String,
    val mimeType: String,
    val sourceUris: List<String>
)
