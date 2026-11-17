package com.example.lagallens.data.datasource.remote

import com.example.lagallens.core.network.AuthSessionScope
import com.example.lagallens.data.datasource.remote.api.PaymentApi
import com.example.lagallens.data.model.request.InitiatePackagePaymentRequest

class PaymentRemoteDataSource(private val api: Lazy<PaymentApi>) {
    suspend fun initiate(scope: AuthSessionScope, idempotencyKey: String, packageId: Long) =
        api.value.initiate(scope, idempotencyKey, InitiatePackagePaymentRequest(packageId)).requireData("Payment")

    suspend fun bankTransfer(scope: AuthSessionScope, paymentId: Long) =
        api.value.bankTransfer(scope, paymentId).requireData("Bank transfer")

    suspend fun status(scope: AuthSessionScope, paymentId: Long) = api.value.status(scope, paymentId).requireData("Payment status")

    suspend fun cancel(scope: AuthSessionScope, paymentId: Long) = api.value.cancel(scope, paymentId).requireData("Payment cancellation")
}
