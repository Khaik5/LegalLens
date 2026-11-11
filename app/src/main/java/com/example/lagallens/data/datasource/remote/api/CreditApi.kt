package com.example.lagallens.data.datasource.remote.api

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.CreditPackageResponse
import com.example.lagallens.data.model.response.CreditTransactionPageResponse
import com.example.lagallens.data.model.response.CreditWalletResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Tag

interface CreditApi {
    @GET("credits/wallet")
    suspend fun wallet(@Tag scope: AuthSessionScope): ApiEnvelope<CreditWalletResponse>

    /** Newest first. */
    @GET("credits/transactions")
    suspend fun transactions(
        @Tag scope: AuthSessionScope,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = HISTORY_SIZE
    ): ApiEnvelope<CreditTransactionPageResponse>

    @GET("credits/packages")
    suspend fun packages(@Tag scope: AuthSessionScope): ApiEnvelope<List<CreditPackageResponse>>

    companion object {
        /** The history screen shows the newest transactions only. */
        const val HISTORY_SIZE = 100
    }
}
