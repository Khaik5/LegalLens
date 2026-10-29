package com.example.lagallens.domain.model

data class NotificationRecord(
    val id: String,
    val contractId: String?,
    val kind: NotificationKind,
    val isUnread: Boolean
)

enum class NotificationKind { ANALYSIS, OCR, REMINDER, SYSTEM }
