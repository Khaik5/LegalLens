package com.example.lagallens.presentation.feature.notifications.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lagallens.R
import com.example.lagallens.di.RepositoryModule
import com.example.lagallens.domain.model.NotificationKind
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
import kotlinx.coroutines.flow.collectLatest
import com.example.lagallens.LegalLensApplication

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LegalLensApplication
    private var loadedSession: String? = app.currentDemoSessionKey()
    private val repository = RepositoryModule.provideNotificationRepository(application)
    private var allNotifications = emptyList<NotificationItem>()
    private val _uiState = MutableStateFlow(NotificationUiState(notifications = allNotifications))
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<NotificationUiEffect>()
    val uiEffect = _uiEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            app.observeDemoSessionKeys().collectLatest { session ->
                if (session != loadedSession) {
                    loadedSession = session
                    allNotifications = emptyList()
                    _uiState.value = NotificationUiState(notifications = emptyList())
                    if (session != null) loadNotifications()
                }
            }
        }
    }

    fun onEvent(event: NotificationUiEvent) {
        if (loadedSession != app.currentDemoSessionKey()) {
            loadedSession = app.currentDemoSessionKey()
            allNotifications = emptyList()
            _uiState.value = NotificationUiState(notifications = emptyList())
        }
        when (event) {
            NotificationUiEvent.ScreenOpened -> viewModelScope.launch { loadNotifications() }
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
        val session = app.currentDemoSessionKey()
        viewModelScope.launch {
            if (session != app.currentDemoSessionKey()) return@launch
            if (runCatching { repository.markRead(notificationId) }.isFailure) {
                if (session != app.currentDemoSessionKey()) return@launch
                _uiState.update { state -> state.copy(errorMessageRes = R.string.auth_backend_not_configured) }
                return@launch
            }
            if (session != app.currentDemoSessionKey()) return@launch
            allNotifications = allNotifications.map { item -> if (item.id == notificationId) item.copy(isUnread = false) else item }
            _uiState.update { state -> state.copy(notifications = filterNotifications(state.selectedFilter)) }
            notification.contractId?.let(::navigateToContractDetail)
        }
    }

    private fun markAllRead() {
        val session = app.currentDemoSessionKey()
        viewModelScope.launch {
            if (session != app.currentDemoSessionKey()) return@launch
            if (runCatching { repository.markAllRead() }.isFailure) {
                if (session != app.currentDemoSessionKey()) return@launch
                _uiState.update { state -> state.copy(errorMessageRes = R.string.auth_backend_not_configured) }
                return@launch
            }
            if (session != app.currentDemoSessionKey()) return@launch
            allNotifications = allNotifications.map { notification -> notification.copy(isUnread = false) }
            _uiState.update { state -> state.copy(notifications = filterNotifications(state.selectedFilter)) }
            _uiEffect.emit(NotificationUiEffect.ShowMessage(R.string.notification_all_marked_read))
        }
    }

    private suspend fun loadNotifications() {
        val session = app.currentDemoSessionKey()
        _uiState.update { it.copy(errorMessageRes = null) }
        runCatching { repository.getAll() }
            .onSuccess { records ->
                if (session != app.currentDemoSessionKey()) return@onSuccess
                val presentationSamples = NotificationItem.samples.associateBy { it.id }
                allNotifications = records.mapNotNull { record ->
                    presentationSamples[record.id]?.copy(
                        contractId = record.contractId,
                        category = record.kind.toPresentationCategory(),
                        isUnread = record.isUnread
                    )
                }
                _uiState.update { it.copy(notifications = filterNotifications(it.selectedFilter)) }
            }
            .onFailure {
                if (session != app.currentDemoSessionKey()) return@onFailure
                allNotifications = emptyList()
                _uiState.update { state -> state.copy(notifications = emptyList(), errorMessageRes = R.string.auth_backend_not_configured) }
            }
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

    private fun navigateToContractDetail(contractId: String) {
        val session = app.currentDemoSessionKey()
        viewModelScope.launch {
            if (session == app.currentDemoSessionKey()) _uiEffect.emit(NotificationUiEffect.NavigateToContractDetail(contractId))
        }
    }
}
