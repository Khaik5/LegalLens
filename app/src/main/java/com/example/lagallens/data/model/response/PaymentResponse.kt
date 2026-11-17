package com.example.lagallens.data.model.response

data class PackagePaymentResponse(val paymentId: Long, val status: String?)

/** [expiresAt] is an ISO instant; older backends do not send it. */
data class BankTransferResponse(val paymentId: Long, val amountVnd: Long, val paymentCode: String, val qrUrl: String, val expiresAt: String? = null)

/** [status] is PENDING, SUCCESS, FAILED, CANCELLED or REFUNDED. */
data class PaymentStatusResponse(val id: Long?, val status: String)
