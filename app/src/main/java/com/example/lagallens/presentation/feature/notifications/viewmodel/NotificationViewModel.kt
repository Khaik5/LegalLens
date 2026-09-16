package com.example.lagallens.presentation.feature.notifications.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.presentation.feature.notifications.contract.NotificationCategory
import com.example.lagallens.presentation.feature.notifications.contract.NotificationFilter
import com.example.lagallens.presentation.feature.notifications.contract.NotificationItem
import com.example.lagallens.presentation.feature.notifications.contract.NotificationUiEffect
import com.example.lagallens.presentation.feature.notifications.contract.NotificationUiEvent
import com.example.lagallens.presentation.feature.notifications.contract.NotificationUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private var allNotifications = emptyList<NotificationItem>()
    private val _uiState = MutableStateFlow(NotificationUiState(notifications = allNotifications))
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<NotificationUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    fun onEvent(event: NotificationUiEvent) {
        when (event) {
            is NotificationUiEvent.FilterSelected -> selectFilter(event.filter)
            is NotificationUiEvent.NotificationClicked -> markNotificationRead(event.notificationId)
            NotificationUiEvent.MarkAllReadClicked -> markAllRead()
        }
    }

    private fun selectFilter(filter: NotificationFilter) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedFilter = filter,
                notifications = filterNotifications(filter)
            )
        }
    }

    private fun markNotificationRead(notificationId: String) {
        val notification = allNotifications.find { it.id == notificationId } ?: return
        allNotifications = allNotifications.map { notification ->
            if (notification.id == notificationId) notification.copy(isUnread = false) else notification
        }
        _uiState.update { currentState ->
            currentState.copy(
                notifications = filterNotifications(currentState.selectedFilter)
            )
        }
        notification.contractId?.let(::navigateToContractDetail)
    }

    private fun markAllRead() {
        allNotifications = allNotifications.map { notification -> notification.copy(isUnread = false) }
        _uiState.update { currentState ->
            currentState.copy(
                notifications = filterNotifications(currentState.selectedFilter)
            )
        }
        viewModelScope.launch {
            _uiEffect.emit(NotificationUiEffect.ShowMessage(R.string.notification_all_marked_read))
        }
    }

    private fun filterNotifications(filter: NotificationFilter): List<NotificationItem> = when (filter) {
        NotificationFilter.ALL -> allNotifications
        NotificationFilter.ANALYSIS -> allNotifications.filter { it.category == NotificationCategory.ANALYSIS }
        NotificationFilter.OCR -> allNotifications.filter { it.category == NotificationCategory.OCR }
        NotificationFilter.REMINDER -> allNotifications.filter { it.category == NotificationCategory.REMINDER }
    }

    private fun navigateToContractDetail(contractId: String) {
        viewModelScope.launch {
            _uiEffect.emit(NotificationUiEffect.NavigateToContractDetail(contractId))
        }
    }
}
