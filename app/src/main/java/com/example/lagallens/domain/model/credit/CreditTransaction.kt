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

data class PurchaseBalance(val before: Int, val after: Int)

/** Walks back from the current balance, so later spending cannot change an earlier receipt. */
fun List<CreditTransaction>.purchaseBalance(paymentId: String, currentBalance: Int): PurchaseBalance? {
    val newestFirst = sortedByDescending { it.createdAtEpochMillis }
    val index = newestFirst.indexOfFirst { it.paymentId == paymentId }
    if (index < 0) return null
    val after = currentBalance - newestFirst.take(index).sumOf { it.creditDelta }
    return PurchaseBalance(after - newestFirst[index].creditDelta, after)
}
