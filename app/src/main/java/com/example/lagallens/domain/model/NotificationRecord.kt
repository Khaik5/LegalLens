package com.example.lagallens.domain.model

data class NotificationRecord(
    val id: String,
    val contractId: String?,
    val kind: NotificationKind,
    val isUnread: Boolean,
    val title: String,
    val description: String,
    val createdAtMillis: Long,
    /** An analysis notification that reports high-risk findings rather than a plain completion. */
    val isHighRisk: Boolean = false
)

enum class NotificationKind { ANALYSIS, OCR, REMINDER, SYSTEM }
