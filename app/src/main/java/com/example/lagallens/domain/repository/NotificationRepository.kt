package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.NotificationRecord

interface NotificationRepository {
    suspend fun getAll(): List<NotificationRecord>
    suspend fun markRead(notificationId: String)
    suspend fun markAllRead()
    suspend fun getAllForSession(sessionKey: String?): List<NotificationRecord> = getAll()
    suspend fun markReadForSession(sessionKey: String?, notificationId: String) = markRead(notificationId)
    suspend fun markAllReadForSession(sessionKey: String?) = markAllRead()
}
