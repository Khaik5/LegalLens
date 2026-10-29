package com.example.lagallens.domain.model.payment

enum class PaymentStatus { PENDING, SUCCESS, FAILED, CANCELLED }

data class PaymentRecord(
    val paymentId: String,
    val packageId: String,
    val amountVnd: Long,
    val credits: Int,
    val status: PaymentStatus,
    val updatedAtEpochMillis: Long
)
