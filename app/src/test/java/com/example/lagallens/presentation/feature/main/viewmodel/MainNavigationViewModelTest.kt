package com.example.lagallens.presentation.feature.main.viewmodel

import com.example.lagallens.MainDispatcherRule
import com.example.lagallens.domain.model.NotificationRecord
import com.example.lagallens.domain.repository.NotificationRepository
import com.example.lagallens.presentation.feature.main.contract.MainNavigationUiEvent
import com.example.lagallens.presentation.feature.main.contract.MainNavigationUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class MainNavigationViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    private val session = MutableStateFlow<String?>("a")
    private val repository = CountFixture()
    private fun model() = MainNavigationViewModel(repository, session) { session.value }

    @Test fun badgeShowsTheServerCountAndHidesAtZero() = runTest {
        repository.serverCount = 3
        val model = model()

        assertEquals("3", model.uiState.value.badgeText)
        repository.serverCount = 0
        model.onEvent(MainNavigationUiEvent.ScreenResumed)
        assertNull(model.uiState.value.badgeText)
    }

    @Test fun largeCountsAreCapped() {
        assertEquals("99+", MainNavigationUiState(unreadNotifications = 120).badgeText)
        assertEquals("99", MainNavigationUiState(unreadNotifications = 99).badgeText)
    }

    @Test fun signingOutClearsTheBadgeAndANewAccountLoadsItsOwn() = runTest {
        repository.serverCount = 5
        val model = model()
        assertEquals(5, model.uiState.value.unreadNotifications)

        session.value = null
        assertEquals(0, model.uiState.value.unreadNotifications)

        repository.serverCount = 2
        session.value = "b"
        assertEquals(2, model.uiState.value.unreadNotifications)
        assertEquals(listOf("a", "b"), repository.refreshedFor)
    }

    @Test fun aFailedRefreshKeepsTheLastValue() = runTest {
        repository.serverCount = 4
        val model = model()
        repository.failure = IllegalStateException("offline")

        model.onEvent(MainNavigationUiEvent.ScreenResumed)

        assertEquals(4, model.uiState.value.unreadNotifications)
    }

    private class CountFixture : NotificationRepository {
        val count = MutableStateFlow(0)
        var serverCount = 0
        var failure: Exception? = null
        val refreshedFor = mutableListOf<String?>()

        override fun observeUnreadCount() = count
        override suspend fun refreshUnreadCount(sessionKey: String?) {
            failure?.let { throw it }
            refreshedFor += sessionKey
            count.value = serverCount
        }

        override suspend fun getAll(): List<NotificationRecord> = emptyList()
        override suspend fun markRead(notificationId: String) = Unit
        override suspend fun markAllRead() = Unit
    }
}
