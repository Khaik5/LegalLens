package com.example.lagallens.data.repository

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.local.datastore.AuthSessionStore
import com.example.lagallens.data.datasource.local.datastore.currentSessionScope
import com.example.lagallens.data.datasource.remote.NotificationRemoteDataSource
import com.example.lagallens.data.mapper.toNotification
import com.example.lagallens.domain.model.NotificationRecord
import com.example.lagallens.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class RemoteNotificationRepository(
    private val tokens: AuthSessionStore,
    private val remote: NotificationRemoteDataSource,
    private val titleFor: (type: String?) -> String
) : NotificationRepository {
    private val unreadCount = MutableStateFlow(0)

    override suspend fun getAll(): List<NotificationRecord> = getAllForSession(null)

    override suspend fun markRead(notificationId: String) = markReadForSession(null, notificationId)

    override suspend fun markAllRead() = markAllReadForSession(null)

    override suspend fun getAllForSession(sessionKey: String?): List<NotificationRecord> = inSession(sessionKey) { scope ->
        remote.list(scope).map { it.toNotification(titleFor) }
    }.also { records -> unreadCount.value = records.count { it.isUnread } }

    override suspend fun markReadForSession(sessionKey: String?, notificationId: String) {
        inSession(sessionKey) { scope -> remote.markRead(scope, notificationId) }
        refreshUnreadCount(sessionKey)
    }

    override suspend fun markAllReadForSession(sessionKey: String?) {
        inSession(sessionKey) { scope -> remote.markAllRead(scope) }
        unreadCount.value = 0
    }

    override fun observeUnreadCount(): Flow<Int> = unreadCount.asStateFlow()

    override suspend fun refreshUnreadCount(sessionKey: String?) {
        // Never show the previous account's count while the new one loads.
        if (tokens.currentSessionScope() == null) {
            unreadCount.value = 0
            return
        }
        unreadCount.value = inSession(sessionKey) { scope -> remote.unreadCount(scope) }.coerceAtLeast(0)
    }

    override suspend fun deleteForSession(sessionKey: String?, notificationId: String) {
        inSession(sessionKey) { scope -> remote.delete(scope, notificationId) }
        refreshUnreadCount(sessionKey)
    }

    /** Runs against [expectedSession] (or the current one) and fails if the account changes meanwhile. */
    private suspend fun <T> inSession(expectedSession: String?, action: suspend (AuthSessionScope) -> T): T =
        withContext(Dispatchers.IO) {
            val scope = checkNotNull(tokens.currentSessionScope()) { "No active session" }
            check(expectedSession == null || expectedSession == scope.key) { "Session changed" }
            val result = action(scope)
            check(tokens.currentSessionScope()?.key == scope.key) { "Session changed" }
            result
        }
}
