package com.example.lagallens.data.datasource.remote.api

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.model.request.ChangePasswordRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.AuthUserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Tag

interface AuthSessionApi {
    @GET("authentication/me")
    suspend fun me(@Tag scope: AuthSessionScope): ApiEnvelope<AuthUserResponse>

    @POST("authentication/logout")
    suspend fun logout(@Tag scope: AuthSessionScope): Response<Unit>

    @PATCH("authentication/password")
    suspend fun changePassword(@Tag scope: AuthSessionScope, @Body request: ChangePasswordRequest): Response<Unit>
}
