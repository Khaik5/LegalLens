package com.example.lagallens.data.datasource.remote.api

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.model.request.AnalysisJobRequest
import com.example.lagallens.data.model.response.AnalysisJobCreatedResponse
import com.example.lagallens.data.model.response.AnalysisJobResponse
import com.example.lagallens.data.model.response.AnalysisLatestResponse
import com.example.lagallens.data.model.response.AnalysisResultResponse
import com.example.lagallens.data.model.response.AnalysisUnlockResponse
import com.example.lagallens.data.model.response.AnalysisUnlockStatusResponse
import com.example.lagallens.data.model.response.ApiEnvelope
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

/** The path ID of `analysis/jobs` is the job ID (a UUID); the app uses it as the result ID. */
interface AnalysisApi {
    @POST("analysis/jobs")
    suspend fun create(
        @Tag scope: AuthSessionScope,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: AnalysisJobRequest
    ): ApiEnvelope<AnalysisJobCreatedResponse>

    @GET("analysis/jobs/{id}")
    suspend fun job(@Tag scope: AuthSessionScope, @Path("id") jobId: String): ApiEnvelope<AnalysisJobResponse>

    /** Without [view] the backend returns the full result when unlocked and the preview otherwise. */
    @GET("analysis/jobs/{id}/result")
    suspend fun result(
        @Tag scope: AuthSessionScope,
        @Path("id") jobId: String,
        @Query("view") view: String? = null
    ): ApiEnvelope<AnalysisResultResponse>

    @GET("analysis/jobs/{id}/unlock")
    suspend fun unlockStatus(@Tag scope: AuthSessionScope, @Path("id") jobId: String): ApiEnvelope<AnalysisUnlockStatusResponse>

    @POST("analysis/jobs/{id}/unlock")
    suspend fun unlock(@Tag scope: AuthSessionScope, @Path("id") jobId: String): ApiEnvelope<AnalysisUnlockResponse>

    @GET("contracts/{id}/analysis/latest")
    suspend fun latest(@Tag scope: AuthSessionScope, @Path("id") contractId: String): ApiEnvelope<AnalysisLatestResponse>
}
