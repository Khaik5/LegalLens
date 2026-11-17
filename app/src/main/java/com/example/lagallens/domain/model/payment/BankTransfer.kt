package com.example.lagallens.domain.model.payment

/** What the user needs to pay a pending package by bank transfer; [expiresAtEpochMillis] is null when the backend sets no limit. */
data class BankTransfer(
    val paymentId: String,
    val amountVnd: Long,
    val paymentCode: String,
    val qrUrl: String,
    val expiresAtEpochMillis: Long?
)

/** The backend cannot give transfer instructions, for example when no receiving account is configured. */
class BankTransferUnavailableException : IllegalStateException("Bank transfer unavailable")
