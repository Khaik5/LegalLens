package com.example.lagallens.presentation.feature.notifications.contract

sealed interface NotificationUiEvent {
    data class FilterSelected(val filter: NotificationFilter) : NotificationUiEvent
    data class NotificationClicked(val notificationId: String) : NotificationUiEvent
    data object MarkAllReadClicked : NotificationUiEvent
}
