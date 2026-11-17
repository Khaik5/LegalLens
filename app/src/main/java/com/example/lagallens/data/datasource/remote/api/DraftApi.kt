package com.example.lagallens.data.datasource.remote.api

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.model.request.CreateDraftRequest
import com.example.lagallens.data.model.request.DraftAiJobRequest
import com.example.lagallens.data.model.request.UpdateDraftRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.DraftAiJobCreatedResponse
import com.example.lagallens.data.model.response.DraftAiJobResponse
import com.example.lagallens.data.model.response.DraftDetailResponse
import com.example.lagallens.data.model.response.DraftPageResponse
import com.example.lagallens.data.model.response.DraftSavedResponse
import com.example.lagallens.data.model.response.DraftVersionListResponse
import com.example.lagallens.data.model.response.DraftVersionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

interface DraftApi {
    @GET("drafts")
    suspend fun list(@Tag scope: AuthSessionScope, @Query("page") page: Int = 1, @Query("limit") limit: Int = 20): ApiEnvelope<DraftPageResponse>

    @POST("drafts")
    suspend fun create(@Tag scope: AuthSessionScope, @Body request: CreateDraftRequest): ApiEnvelope<DraftSavedResponse>

    @GET("drafts/{id}")
    suspend fun get(@Tag scope: AuthSessionScope, @Path("id") draftId: Long): ApiEnvelope<DraftDetailResponse>

    @PATCH("drafts/{id}")
    suspend fun update(@Tag scope: AuthSessionScope, @Path("id") draftId: Long, @Body request: UpdateDraftRequest): ApiEnvelope<DraftSavedResponse>

    /** 204 without a body. */
    @DELETE("drafts/{id}")
    suspend fun delete(@Tag scope: AuthSessionScope, @Path("id") draftId: Long): Response<Unit>

    @POST("drafts/{id}/copy")
    suspend fun copy(@Tag scope: AuthSessionScope, @Path("id") draftId: Long): ApiEnvelope<DraftSavedResponse>

    @GET("drafts/{id}/versions")
    suspend fun versions(@Tag scope: AuthSessionScope, @Path("id") draftId: Long, @Query("limit") limit: Int = 50): ApiEnvelope<DraftVersionListResponse>

    @GET("drafts/{id}/versions/{version}")
    suspend fun version(@Tag scope: AuthSessionScope, @Path("id") draftId: Long, @Path("version") version: Int): ApiEnvelope<DraftVersionResponse>

    @POST("draft-ai-jobs")
    suspend fun createJob(
        @Tag scope: AuthSessionScope,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: DraftAiJobRequest
    ): ApiEnvelope<DraftAiJobCreatedResponse>

    @GET("draft-ai-jobs/{id}")
    suspend fun job(@Tag scope: AuthSessionScope, @Path("id") jobId: String): ApiEnvelope<DraftAiJobResponse>

    @POST("draft-ai-jobs/{id}/apply")
    suspend fun apply(@Tag scope: AuthSessionScope, @Path("id") jobId: String): ApiEnvelope<DraftSavedResponse>

    @POST("draft-ai-jobs/{id}/cancel")
    suspend fun cancel(@Tag scope: AuthSessionScope, @Path("id") jobId: String): ApiEnvelope<DraftAiJobCreatedResponse>
}
