package com.example.lagallens.presentation.feature.notifications.contract

sealed interface NotificationUiEvent {
    data object ScreenOpened : NotificationUiEvent
    data class FilterSelected(val filter: NotificationFilter) : NotificationUiEvent
    data class NotificationClicked(val notificationId: String) : NotificationUiEvent
    data object MarkAllReadClicked : NotificationUiEvent
    /** Hidden at once; deleted on the server only after the undo window closes. */
    data class NotificationSwiped(val notificationId: String) : NotificationUiEvent
    data class UndoDeleteClicked(val notificationId: String) : NotificationUiEvent
    data class DeleteUndoWindowClosed(val notificationId: String) : NotificationUiEvent
    /** The screen is no longer visible, so a swiped notification is deleted now. */
    data object ScreenStopped : NotificationUiEvent
}
