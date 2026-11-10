package com.example.lagallens.data.datasource.remote

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.api.NotificationApi
import com.example.lagallens.data.model.response.NotificationResponse
import retrofit2.HttpException
import retrofit2.Response

class NotificationRemoteDataSource(private val api: Lazy<NotificationApi>) {
    /** Reads every page; the backend returns the newest notifications first. */
    suspend fun list(scope: AuthSessionScope): List<NotificationResponse> {
        val notifications = mutableListOf<NotificationResponse>()
        var page = 1
        while (true) {
            val response = api.value.list(scope, page).requireData("Notifications")
            notifications.addAll(response.data)
            if (response.data.isEmpty() || page.toLong() * response.limit >= response.total) break
            check(response.limit > 0) { "Invalid notification page size" }
            page++
        }
        return notifications.distinctBy { it.id }
    }

    suspend fun markRead(scope: AuthSessionScope, id: String) = api.value.markRead(scope, id).requireSuccess()

    suspend fun markAllRead(scope: AuthSessionScope) = api.value.markAllRead(scope).requireSuccess()

    private fun Response<Unit>.requireSuccess() {
        if (!isSuccessful) throw HttpException(this)
    }
}
