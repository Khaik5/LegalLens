package com.example.lagallens.data.model.response

data class NotificationResponse(
    val id: Long,
    val type: String?,
    val message: String?,
    val isRead: Boolean,
    val contractId: Long?,
    val createdAt: String
)

data class NotificationPageResponse(
    val data: List<NotificationResponse>,
    val total: Long,
    val page: Int,
    val limit: Int
)
