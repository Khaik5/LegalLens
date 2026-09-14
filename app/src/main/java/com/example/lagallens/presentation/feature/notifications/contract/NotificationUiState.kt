package com.example.lagallens.presentation.feature.notifications.contract

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.lagallens.R

data class NotificationUiState(
    val selectedFilter: NotificationFilter = NotificationFilter.ALL,
    val notifications: List<NotificationItem> = emptyList()
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
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val timeRes: Int,
    val category: NotificationCategory,
    @DrawableRes val iconRes: Int,
    @DrawableRes val iconBackgroundRes: Int,
    @ColorRes val iconTintRes: Int,
    val isUnread: Boolean
) {
    companion object {
        val samples = listOf(
            NotificationItem(
                id = "analysis-bitexco",
                contractId = "office-lease",
                titleRes = R.string.notification_analysis_complete_title,
                descriptionRes = R.string.notification_analysis_complete_description,
                timeRes = R.string.notification_ten_minutes_ago,
                category = NotificationCategory.ANALYSIS,
                iconRes = R.drawable.ic_shield_check,
                iconBackgroundRes = R.drawable.bg_notification_icon_complete,
                iconTintRes = R.color.contract_complete,
                isUnread = true
            ),
            NotificationItem(
                id = "ocr-collaborator",
                contractId = "collaborator",
                titleRes = R.string.notification_ocr_review_title,
                descriptionRes = R.string.notification_ocr_review_description,
                timeRes = R.string.notification_one_hour_ago,
                category = NotificationCategory.OCR,
                iconRes = R.drawable.ic_camera_warning,
                iconBackgroundRes = R.drawable.bg_notification_icon_pending,
                iconTintRes = R.color.contract_pending,
                isUnread = true
            ),
            NotificationItem(
                id = "reminder-bitexco",
                titleRes = R.string.notification_expiring_title,
                descriptionRes = R.string.notification_expiring_description,
                timeRes = R.string.notification_three_hours_ago,
                category = NotificationCategory.REMINDER,
                iconRes = R.drawable.ic_dashboard_calendar,
                iconBackgroundRes = R.drawable.bg_notification_icon_reminder,
                iconTintRes = R.color.notification_blue,
                isUnread = false
            ),
            NotificationItem(
                id = "document-ready",
                contractId = "office-lease",
                titleRes = R.string.notification_document_ready_title,
                descriptionRes = R.string.notification_document_ready_description,
                timeRes = R.string.notification_yesterday,
                category = NotificationCategory.ANALYSIS,
                iconRes = R.drawable.ic_contract_search_document,
                iconBackgroundRes = R.drawable.bg_notification_icon_document,
                iconTintRes = R.color.legal_lens_auth_accent,
                isUnread = false
            ),
            NotificationItem(
                id = "system-maintenance",
                titleRes = R.string.notification_maintenance_title,
                descriptionRes = R.string.notification_maintenance_description,
                timeRes = R.string.notification_two_days_ago,
                category = NotificationCategory.SYSTEM,
                iconRes = R.drawable.ic_notification_info,
                iconBackgroundRes = R.drawable.bg_notification_icon_system,
                iconTintRes = R.color.legal_lens_secondary_text,
                isUnread = false
            )
        )
    }
}
