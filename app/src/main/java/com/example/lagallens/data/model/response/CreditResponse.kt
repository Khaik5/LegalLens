package com.example.lagallens.data.model.response

/** Credit amounts are decimals with up to two places. */
data class CreditWalletResponse(
    val id: Long,
    val userId: Long,
    val balance: Double,
    val updatedAt: String?
)

data class CreditTransactionResponse(
    val id: Long,
    /** PURCHASE, USAGE, REFUND or BONUS. */
    val type: String?,
    /** Signed: usage is negative. */
    val amount: Double,
    val balanceAfter: Double,
    val paymentId: Long?,
    val note: String?,
    val createdAt: String?
)

data class CreditTransactionPageResponse(
    val data: List<CreditTransactionResponse>,
    val total: Long,
    val page: Int,
    val limit: Int
)

data class CreditPackageResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val credits: Int,
    val bonusCredits: Int?,
    val priceVnd: Long,
    val displayOrder: Int?
)
