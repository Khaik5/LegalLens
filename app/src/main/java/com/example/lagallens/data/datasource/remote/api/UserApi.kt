package com.example.lagallens.data.datasource.remote.api

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.model.request.UpdateUserRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.UserResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Tag

interface UserApi {
    @GET("users/me")
    suspend fun me(@Tag scope: AuthSessionScope): ApiEnvelope<UserResponse>

    @PATCH("users/{id}")
    suspend fun update(
        @Tag scope: AuthSessionScope,
        @Path("id") id: String,
        @Body request: UpdateUserRequest
    ): ApiEnvelope<UserResponse>
}
