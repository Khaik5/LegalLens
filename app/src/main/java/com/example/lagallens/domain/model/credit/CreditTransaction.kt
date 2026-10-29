package com.example.lagallens.domain.model.credit

data class CreditTransaction(
    val transactionId: String,
    val paymentId: String?,
    val packageId: String?,
    val amountVnd: Long,
    val creditDelta: Int,
    val status: String,
    val createdAtEpochMillis: Long
)

data class CreditReceipt(val balance: Int, val applied: Boolean)
