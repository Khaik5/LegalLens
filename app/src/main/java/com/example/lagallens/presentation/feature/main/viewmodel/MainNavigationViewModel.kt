package com.example.lagallens.presentation.feature.main.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lagallens.domain.repository.NotificationRepository
import com.example.lagallens.presentation.feature.main.contract.MainNavigationUiEvent
import com.example.lagallens.presentation.feature.main.contract.MainNavigationUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainNavigationViewModel(
    private val notifications: NotificationRepository,
    private val sessionKeys: Flow<String?>,
    private val currentSession: () -> String?
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainNavigationUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            notifications.observeUnreadCount().collect { count ->
                _uiState.update { it.copy(unreadNotifications = count) }
            }
        }
        viewModelScope.launch {
            sessionKeys.distinctUntilChanged().collect { session ->
                // Another account must never see the previous account's count.
                _uiState.value = MainNavigationUiState()
                if (session != null) refreshUnreadCount()
            }
        }
    }

    fun onEvent(event: MainNavigationUiEvent) {
        when (event) {
            MainNavigationUiEvent.ScreenResumed -> viewModelScope.launch { refreshUnreadCount() }
        }
    }

    private suspend fun refreshUnreadCount() {
        val session = currentSession() ?: return
        try {
            notifications.refreshUnreadCount(session)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // The badge keeps its last value; it is a hint and the Notifications tab shows any error.
        }
    }

    class Factory(
        private val notifications: NotificationRepository,
        private val sessionKeys: Flow<String?>,
        private val currentSession: () -> String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainNavigationViewModel(notifications, sessionKeys, currentSession) as T
    }
}
