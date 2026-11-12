package com.example.lagallens.data.datasource.remote.api

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.model.request.OcrCorrectionRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.OcrCorrectionResponse
import com.example.lagallens.data.model.response.OcrJobResponse
import com.example.lagallens.data.model.response.OcrResultResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Tag

/** The path ID is the uploaded document (file) ID, not the contract ID. */
interface OcrApi {
    @POST("documents/{id}/ocr")
    suspend fun create(
        @Tag scope: AuthSessionScope,
        @Path("id") documentId: String,
        @Header("Idempotency-Key") idempotencyKey: String
    ): ApiEnvelope<OcrJobResponse>

    @GET("documents/{id}/ocr")
    suspend fun latest(@Tag scope: AuthSessionScope, @Path("id") documentId: String): ApiEnvelope<OcrResultResponse>

    @PATCH("documents/{id}/ocr")
    suspend fun correct(
        @Tag scope: AuthSessionScope,
        @Path("id") documentId: String,
        @Body request: OcrCorrectionRequest
    ): ApiEnvelope<OcrCorrectionResponse>
}
