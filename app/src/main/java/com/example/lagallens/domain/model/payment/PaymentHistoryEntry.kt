package com.example.lagallens.domain.model.payment

/** One Credit purchase of the signed-in user, as the backend recorded it. */
data class PaymentHistoryEntry(
    val paymentId: String,
    val credits: Int,
    val amountVnd: Long,
    val status: PaymentStatus,
    val createdAtEpochMillis: Long
)
