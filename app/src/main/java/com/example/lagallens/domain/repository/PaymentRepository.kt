package com.example.lagallens.domain.repository

import com.example.lagallens.domain.model.payment.PaymentRecord
import com.example.lagallens.domain.model.payment.PaymentStatus

interface PaymentRepository {
    fun activeSessionKey(): String? = null
    val isConfigured: Boolean get() = true
    suspend fun createOrGet(paymentId: String, packageId: String, amountVnd: Long, credits: Int): PaymentRecord
    suspend fun updateStatus(paymentId: String, status: PaymentStatus): PaymentRecord
    suspend fun get(paymentId: String): PaymentRecord?
    suspend fun completeDemoPayment(paymentId: String): PaymentRecord
}
