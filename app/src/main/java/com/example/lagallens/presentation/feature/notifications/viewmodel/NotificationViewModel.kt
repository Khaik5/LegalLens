package com.example.lagallens.presentation.feature.notifications.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.presentation.common.utils.toFailureMessage
import com.example.lagallens.R
import com.example.lagallens.domain.model.NotificationKind
import com.example.lagallens.domain.model.NotificationRecord
import com.example.lagallens.presentation.feature.notifications.contract.NotificationCategory
import com.example.lagallens.presentation.feature.notifications.contract.NotificationFilter
import com.example.lagallens.presentation.feature.notifications.contract.NotificationItem
import com.example.lagallens.presentation.feature.notifications.contract.NotificationUiEffect
import com.example.lagallens.presentation.feature.notifications.contract.NotificationUiEvent
import com.example.lagallens.presentation.feature.notifications.contract.NotificationUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import com.example.lagallens.LegalLensApplication

class NotificationViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: com.example.lagallens.domain.repository.NotificationRepository = (application as LegalLensApplication).notificationRepository
) : AndroidViewModel(application) {
    private val app = application as LegalLensApplication
    private var loadedSession: String? = app.currentSessionKey()
    private var allNotifications = emptyList<NotificationItem>()
    /** Swiped but not yet deleted on the server, with its position so Undo puts it back in place. */
    private var pendingDelete: Pair<Int, NotificationItem>? = null
    private val _uiState = MutableStateFlow(NotificationUiState(notifications = allNotifications))
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<NotificationUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        viewModelScope.launch {
            app.observeSessionKeys().collectLatest { session ->
                if (session != loadedSession) {
                    loadedSession = session
                    allNotifications = emptyList()
                    _uiState.value = NotificationUiState(notifications = emptyList())
                    if (session != null) loadNotifications(session)
                }
            }
        }
    }

    fun onEvent(event: NotificationUiEvent) {
        if (loadedSession != app.currentSessionKey()) {
            loadedSession = app.currentSessionKey()
            allNotifications = emptyList()
            _uiState.value = NotificationUiState(notifications = emptyList())
        }
        when (event) {
            NotificationUiEvent.ScreenOpened -> {
                val session = loadedSession
                viewModelScope.launch { loadNotifications(session) }
            }
            is NotificationUiEvent.FilterSelected -> selectFilter(event.filter)
            is NotificationUiEvent.NotificationClicked -> markNotificationRead(event.notificationId)
            NotificationUiEvent.MarkAllReadClicked -> markAllRead()
            is NotificationUiEvent.NotificationSwiped -> hideForDelete(event.notificationId)
            is NotificationUiEvent.UndoDeleteClicked -> undoDelete(event.notificationId)
            is NotificationUiEvent.DeleteUndoWindowClosed -> if (pendingDelete?.second?.id == event.notificationId) commitPendingDelete()
            NotificationUiEvent.ScreenStopped -> commitPendingDelete()
        }
    }

    private fun hideForDelete(notificationId: String) {
        // Only one undo is offered; an earlier swipe is deleted now.
        commitPendingDelete()
        val index = allNotifications.indexOfFirst { it.id == notificationId }
        if (index < 0) return
        pendingDelete = index to allNotifications[index]
        allNotifications = allNotifications.filterNot { it.id == notificationId }
        _uiState.update { state -> state.copy(notifications = filterNotifications(state.selectedFilter)) }
        viewModelScope.launch { _uiEffect.send(NotificationUiEffect.ShowDeletedWithUndo(notificationId)) }
    }

    private fun undoDelete(notificationId: String) {
        val (index, item) = pendingDelete?.takeIf { it.second.id == notificationId } ?: return
        pendingDelete = null
        restore(index, item)
    }

    private fun restore(index: Int, item: NotificationItem) {
        if (allNotifications.any { it.id == item.id }) return
        allNotifications = allNotifications.toMutableList().apply { add(index.coerceIn(0, size), item) }
        _uiState.update { state -> state.copy(notifications = filterNotifications(state.selectedFilter)) }
    }

    private fun commitPendingDelete() {
        val (index, item) = pendingDelete ?: return
        pendingDelete = null
        val session = app.currentSessionKey()
        viewModelScope.launch {
            val failure = runCatching { repository.deleteForSession(session, item.id) }.exceptionOrNull() ?: return@launch
            if (failure is kotlinx.coroutines.CancellationException) throw failure
            if (session != app.currentSessionKey()) return@launch
            restore(index, item)
            _uiEffect.send(NotificationUiEffect.ShowMessage(R.string.notification_delete_failed))
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
        val session = app.currentSessionKey()
        viewModelScope.launch {
            if (session != app.currentSessionKey()) return@launch
            val failure = runCatching { repository.markReadForSession(session, notificationId) }.exceptionOrNull()
            if (failure != null) {
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                if (session != app.currentSessionKey()) return@launch
                _uiEffect.send(NotificationUiEffect.ShowMessage(failure.toFailureMessage()))
                return@launch
            }
            if (session != app.currentSessionKey()) return@launch
            allNotifications = allNotifications.map { item -> if (item.id == notificationId) item.copy(isUnread = false) else item }
            _uiState.update { state -> state.copy(notifications = filterNotifications(state.selectedFilter), errorMessageRes = null) }
            notification.contractId?.let { navigateToContractDetail(it, session) }
        }
    }

    private fun markAllRead() {
        val session = app.currentSessionKey()
        viewModelScope.launch {
            if (session != app.currentSessionKey()) return@launch
            val failure = runCatching { repository.markAllReadForSession(session) }.exceptionOrNull()
            if (failure != null) {
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                if (session != app.currentSessionKey()) return@launch
                _uiEffect.send(NotificationUiEffect.ShowMessage(failure.toFailureMessage()))
                return@launch
            }
            if (session != app.currentSessionKey()) return@launch
            allNotifications = allNotifications.map { notification -> notification.copy(isUnread = false) }
            _uiState.update { state -> state.copy(notifications = filterNotifications(state.selectedFilter), errorMessageRes = null) }
            _uiEffect.send(NotificationUiEffect.ShowMessage(R.string.notification_all_marked_read))
        }
    }

    private suspend fun loadNotifications(session: String?) {
        if (session != app.currentSessionKey()) return
        _uiState.update { it.copy(errorMessageRes = null) }
        runCatching { repository.getAllForSession(session) }
            .onSuccess { records ->
                if (session != app.currentSessionKey()) return@onSuccess
                allNotifications = records.map { record -> record.toItem() }.filterNot { it.id == pendingDelete?.second?.id }
                _uiState.update { it.copy(notifications = filterNotifications(it.selectedFilter)) }
            }
            .onFailure { failure ->
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                if (session != app.currentSessionKey()) return@onFailure
                allNotifications = emptyList()
                _uiState.update { state -> state.copy(notifications = emptyList(), errorMessageRes = failure.toFailureMessage()) }
            }
    }

    private fun NotificationRecord.toItem(): NotificationItem {
        val (iconRes, iconBackgroundRes, iconTintRes) = when (kind) {
            NotificationKind.ANALYSIS -> Triple(R.drawable.ic_shield_check, R.drawable.bg_notification_icon_complete, R.color.contract_complete)
            NotificationKind.OCR -> Triple(R.drawable.ic_camera_warning, R.drawable.bg_notification_icon_pending, R.color.contract_pending)
            NotificationKind.REMINDER -> Triple(R.drawable.ic_dashboard_calendar, R.drawable.bg_notification_icon_reminder, R.color.notification_blue)
            NotificationKind.SYSTEM -> Triple(R.drawable.ic_notification_info, R.drawable.bg_notification_icon_system, R.color.legal_lens_secondary_text)
        }
        return NotificationItem(id, contractId, title, description, createdAtMillis, kind.toPresentationCategory(), iconRes, iconBackgroundRes, iconTintRes, isUnread)
    }

    private fun NotificationKind.toPresentationCategory() = when (this) {
        NotificationKind.ANALYSIS -> NotificationCategory.ANALYSIS
        NotificationKind.OCR -> NotificationCategory.OCR
        NotificationKind.REMINDER -> NotificationCategory.REMINDER
        NotificationKind.SYSTEM -> NotificationCategory.SYSTEM
    }

    private fun filterNotifications(filter: NotificationFilter): List<NotificationItem> = when (filter) {
        NotificationFilter.ALL -> allNotifications
        NotificationFilter.ANALYSIS -> allNotifications.filter { it.category == NotificationCategory.ANALYSIS }
        NotificationFilter.OCR -> allNotifications.filter { it.category == NotificationCategory.OCR }
        NotificationFilter.REMINDER -> allNotifications.filter { it.category == NotificationCategory.REMINDER }
    }

    private fun navigateToContractDetail(contractId: String, session: String?) {
        viewModelScope.launch {
            if (session == app.currentSessionKey()) _uiEffect.send(NotificationUiEffect.NavigateToContractDetail(contractId))
        }
    }
}
