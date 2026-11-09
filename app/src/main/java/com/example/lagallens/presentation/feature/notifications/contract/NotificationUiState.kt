package com.example.lagallens.presentation.feature.notifications.contract

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.lagallens.R

data class NotificationUiState(
    val selectedFilter: NotificationFilter = NotificationFilter.ALL,
    val notifications: List<NotificationItem> = emptyList(),
    val errorMessageRes: Int? = null
)

enum class NotificationFilter(@StringRes val titleRes: Int) {
    ALL(R.string.contract_filter_all),
    ANALYSIS(R.string.notification_filter_analysis),
    OCR(R.string.notification_filter_ocr),
    REMINDER(R.string.notification_filter_reminder)
}

enum class NotificationCategory {
    ANALYSIS,
    OCR,
    REMINDER,
    SYSTEM
}

data class NotificationItem(
    val id: String,
    val contractId: String? = null,
    val title: String,
    val description: String,
    val createdAtMillis: Long,
    val category: NotificationCategory,
    @DrawableRes val iconRes: Int,
    @DrawableRes val iconBackgroundRes: Int,
    @ColorRes val iconTintRes: Int,
    val isUnread: Boolean
)
