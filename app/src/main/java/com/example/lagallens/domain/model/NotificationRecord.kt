package com.example.lagallens.domain.model

data class NotificationRecord(
    val id: String,
    val contractId: String?,
    val kind: NotificationKind,
    val isUnread: Boolean,
    val title: String,
    val description: String,
    val createdAtMillis: Long
)

enum class NotificationKind { ANALYSIS, OCR, REMINDER, SYSTEM }
