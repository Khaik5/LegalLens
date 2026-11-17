package com.example.lagallens.data.datasource.remote.api

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.model.request.InitiatePackagePaymentRequest
import com.example.lagallens.data.model.response.ApiEnvelope
import com.example.lagallens.data.model.response.BankTransferResponse
import com.example.lagallens.data.model.response.PackagePaymentResponse
import com.example.lagallens.data.model.response.PaymentPageResponse
import com.example.lagallens.data.model.response.PaymentStatusResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

/** Path IDs are the backend's numeric payment IDs, not the IDs the app generates. */
interface PaymentApi {
    @POST("payments/initiate-package")
    suspend fun initiate(
        @Tag scope: AuthSessionScope,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: InitiatePackagePaymentRequest
    ): ApiEnvelope<PackagePaymentResponse>

    /** The user's own payments, newest first. */
    @GET("payments")
    suspend fun list(
        @Tag scope: AuthSessionScope,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = HISTORY_SIZE
    ): ApiEnvelope<PaymentPageResponse>

    @GET("payments/{id}/bank-transfer")
    suspend fun bankTransfer(@Tag scope: AuthSessionScope, @Path("id") paymentId: Long): ApiEnvelope<BankTransferResponse>

    @GET("payments/{id}/status")
    suspend fun status(@Tag scope: AuthSessionScope, @Path("id") paymentId: Long): ApiEnvelope<PaymentStatusResponse>

    @POST("payments/{id}/cancel")
    suspend fun cancel(@Tag scope: AuthSessionScope, @Path("id") paymentId: Long): ApiEnvelope<PaymentStatusResponse>

    companion object {
        /** The history screen shows the newest purchases only. */
        const val HISTORY_SIZE = 50
    }
}
