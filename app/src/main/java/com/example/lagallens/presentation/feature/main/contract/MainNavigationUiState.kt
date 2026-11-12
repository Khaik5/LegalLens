package com.example.lagallens.presentation.feature.main.contract

data class MainNavigationUiState(val unreadNotifications: Int = 0) {
    /** Shown on the Notifications tab; hidden when nothing is unread. */
    val badgeText: String? get() = when {
        unreadNotifications <= 0 -> null
        unreadNotifications > MAX_SHOWN -> "$MAX_SHOWN+"
        else -> unreadNotifications.toString()
    }

    private companion object {
        const val MAX_SHOWN = 99
    }
}
