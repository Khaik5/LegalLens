package com.example.lagallens.data.mapper

import com.example.lagallens.data.model.response.CreditPackageResponse
import com.example.lagallens.data.model.response.CreditTransactionResponse
import com.example.lagallens.domain.model.credit.CreditOffer
import com.example.lagallens.domain.model.credit.CreditTransaction
import com.example.lagallens.domain.model.credit.CreditTransactionKind
import java.time.Instant
import kotlin.math.floor
import kotlin.math.roundToInt

/** Rounds down, so a fractional balance is never shown as more than the account holds. */
internal fun creditBalance(balance: Double): Int = floor(balance).toInt()

/** Backend transactions are applied when they are recorded; VND amounts belong to payments, not transactions. */
internal fun CreditTransactionResponse.toCreditTransaction() = CreditTransaction(
    transactionId = id.toString(),
    paymentId = paymentId?.toString(),
    packageId = null,
    amountVnd = 0,
    creditDelta = amount.roundToInt(),
    status = "COMPLETED",
    createdAtEpochMillis = createdAt?.let { Instant.parse(it).toEpochMilli() } ?: 0L,
    kind = when (type) {
        "PURCHASE" -> CreditTransactionKind.PURCHASE
        "USAGE" -> CreditTransactionKind.USAGE
        "REFUND" -> CreditTransactionKind.REFUND
        "BONUS" -> CreditTransactionKind.BONUS
        else -> null
    }
)

/** A package grants its credits plus any bonus credits. */
internal fun CreditPackageResponse.toCreditOffer() = CreditOffer(
    id = id.toString(),
    credits = credits + (bonusCredits ?: 0),
    priceVnd = priceVnd,
    title = name,
    description = description.orEmpty()
)
