package com.example.lagallens.data.model.request

/** [ocrRevisionId] is a string: the backend rejects a number. */
data class AnalysisJobRequest(val contractId: Long, val documentId: Long, val ocrRevisionId: String)

/** A null [text] confirms the current revision as it is. */
data class OcrConfirmRequest(val expectedRevision: Int, val text: String? = null)
