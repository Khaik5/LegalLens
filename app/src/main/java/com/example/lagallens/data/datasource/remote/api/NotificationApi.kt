package com.example.lagallens.data.datasource.remote.api

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.NotificationPageResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

interface NotificationApi {
    @GET("notifications")
    suspend fun list(
        @Tag scope: AuthSessionScope,
        @Query("page") page: Int,
        @Query("limit") limit: Int = PAGE_SIZE
    ): ApiEnvelope<NotificationPageResponse>

    /** 204 without a body. */
    @PATCH("notifications/{id}/read")
    suspend fun markRead(@Tag scope: AuthSessionScope, @Path("id") id: String): Response<Unit>

    /** 204 without a body. */
    @PATCH("notifications/read-all")
    suspend fun markAllRead(@Tag scope: AuthSessionScope): Response<Unit>

    companion object {
        const val PAGE_SIZE = 100
    }
}
