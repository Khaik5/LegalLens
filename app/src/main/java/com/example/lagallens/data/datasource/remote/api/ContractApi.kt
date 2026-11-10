package com.example.lagallens.data.datasource.remote.api

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.model.request.CreateContractRequest
import com.example.lagallens.data.model.request.UpdateContractRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.ContractPageResponse
import com.example.lagallens.data.model.response.ContractResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

interface ContractApi {
    @GET("contracts/user/{userId}/page")
    suspend fun list(
        @Tag scope: AuthSessionScope,
        @Path("userId") userId: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 100,
        @Query("sort") sort: String = "updated_desc"
    ): ApiEnvelope<ContractPageResponse>

    @GET("contracts/{id}")
    suspend fun get(@Tag scope: AuthSessionScope, @Path("id") id: String): ApiEnvelope<ContractResponse>

    @POST("contracts")
    suspend fun create(@Tag scope: AuthSessionScope, @Body request: CreateContractRequest): ApiEnvelope<ContractResponse>

    @PATCH("contracts/{id}")
    suspend fun update(
        @Tag scope: AuthSessionScope,
        @Path("id") id: String,
        @Body request: UpdateContractRequest
    ): ApiEnvelope<ContractResponse>

    @DELETE("contracts/{id}")
    suspend fun delete(@Tag scope: AuthSessionScope, @Path("id") id: String): ApiEnvelope<Unit>
}
