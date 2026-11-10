package com.example.lagallens.data.mapper

import com.example.lagallens.data.model.response.NotificationResponse
import com.example.lagallens.domain.model.NotificationKind
import com.example.lagallens.domain.model.NotificationRecord
import java.time.Instant

/** The backend sends no title; [titleFor] supplies a localized one for the backend notification type. */
internal fun NotificationResponse.toNotification(titleFor: (type: String?) -> String) = NotificationRecord(
    id = id.toString(),
    contractId = contractId?.toString(),
    kind = when (type) {
        "ANALYSIS_DONE", "HIGH_RISK_FOUND" -> NotificationKind.ANALYSIS
        "CONTRACT_EXPIRING" -> NotificationKind.REMINDER
        else -> NotificationKind.SYSTEM
    },
    isUnread = !isRead,
    title = titleFor(type),
    description = message.orEmpty(),
    createdAtMillis = Instant.parse(createdAt).toEpochMilli()
)
