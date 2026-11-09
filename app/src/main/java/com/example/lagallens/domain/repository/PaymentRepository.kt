package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.credit.CreditOffer
import com.example.lagallens.domain.model.payment.PaymentRecord
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    fun activeSessionKey(): String? = null
    val isConfigured: Boolean get() = true
    suspend fun getPackages(): List<CreditOffer>
    /** Idempotent by paymentId. The amount and credits are decided from packageId by the data source. */
    suspend fun createPayment(paymentId: String, packageId: String): PaymentRecord
    /** Emits the payment as its status changes; confirmation comes from the data source, not the client. */
    fun observePayment(paymentId: String): Flow<PaymentRecord>
    suspend fun cancelPayment(paymentId: String): PaymentRecord
    suspend fun get(paymentId: String): PaymentRecord?
}
