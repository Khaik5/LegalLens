package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.NotificationRecord

interface NotificationRepository {
    suspend fun getAll(): List<NotificationRecord>
    suspend fun markRead(notificationId: String)
    suspend fun markAllRead()
}
