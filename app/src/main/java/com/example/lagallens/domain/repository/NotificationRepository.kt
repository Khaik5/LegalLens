package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.NotificationRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface NotificationRepository {
    suspend fun getAll(): List<NotificationRecord>
    suspend fun markRead(notificationId: String)
    suspend fun markAllRead()
    suspend fun getAllForSession(sessionKey: String?): List<NotificationRecord> = getAll()
    suspend fun markReadForSession(sessionKey: String?, notificationId: String) = markRead(notificationId)
    suspend fun markAllReadForSession(sessionKey: String?) = markAllRead()

    /** Unread count of the signed-in account; listing, reading and deleting keep it current. */
    fun observeUnreadCount(): Flow<Int> = emptyFlow()
    suspend fun refreshUnreadCount(sessionKey: String?) = Unit
    suspend fun deleteForSession(sessionKey: String?, notificationId: String): Unit =
        throw UnsupportedOperationException("Deleting notifications is not supported")
}
