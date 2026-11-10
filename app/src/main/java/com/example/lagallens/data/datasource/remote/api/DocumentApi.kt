package com.example.lagallens.data.datasource.remote.api

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.DocumentResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming
import retrofit2.http.Tag

interface DocumentApi {
    @Multipart
    @POST("documents/upload")
    suspend fun upload(
        @Tag scope: AuthSessionScope,
        @Part("contractId") contractId: RequestBody,
        @Part file: MultipartBody.Part
    ): ApiEnvelope<DocumentResponse>

    @GET("documents/contract/{contractId}")
    suspend fun list(
        @Tag scope: AuthSessionScope,
        @Path("contractId") contractId: String
    ): ApiEnvelope<List<DocumentResponse>>

    @GET("documents/{id}")
    suspend fun get(@Tag scope: AuthSessionScope, @Path("id") id: String): ApiEnvelope<DocumentResponse>

    @DELETE("documents/{id}")
    suspend fun delete(@Tag scope: AuthSessionScope, @Path("id") id: String): ApiEnvelope<Unit>

    @Streaming
    @GET("documents/{id}/preview")
    suspend fun preview(@Tag scope: AuthSessionScope, @Path("id") id: String): ResponseBody

    @Streaming
    @GET("documents/{id}/download")
    suspend fun download(@Tag scope: AuthSessionScope, @Path("id") id: String): ResponseBody
}
