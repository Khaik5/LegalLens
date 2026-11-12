package com.example.lagallens.presentation.feature.main.contract

sealed interface MainNavigationUiEvent {
    /** The app came to the foreground; new notifications may have arrived meanwhile. */
    data object ScreenResumed : MainNavigationUiEvent
}
