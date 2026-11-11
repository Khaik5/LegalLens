package com.example.lagallens.data.datasource.remote.api

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.UserResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming
import retrofit2.http.Tag

interface UserApi {
    @GET("users/me")
    suspend fun me(@Tag scope: AuthSessionScope): ApiEnvelope<UserResponse>

    /** [body] is JSON written with explicit nulls, so a cleared phone number reaches the backend. */
    @PATCH("users/me")
    suspend fun updateMe(@Tag scope: AuthSessionScope, @Body body: RequestBody): ApiEnvelope<UserResponse>

    @Multipart
    @POST("users/me/avatar")
    suspend fun uploadAvatar(@Tag scope: AuthSessionScope, @Part file: MultipartBody.Part): ApiEnvelope<UserResponse>

    @Streaming
    @GET("users/me/avatar")
    suspend fun avatar(@Tag scope: AuthSessionScope): ResponseBody
}
