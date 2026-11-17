package com.example.lagallens.data.model.request

/** Credit packages are paid by bank transfer (VietQR / SePay). */
data class InitiatePackagePaymentRequest(val packageId: Long, val paymentMethod: String = "BANK_TRANSFER")
