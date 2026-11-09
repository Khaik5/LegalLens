package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.Intent
import com.example.lagallens.presentation.feature.creditstore.contract.CreditPackage
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

/** Display data for the screens of one purchase; the amount charged is decided by PaymentRepository from the package id. */
internal data class CreditPurchaseSelection(val creditPackage: CreditPackage, val balanceCredits: Int, val purchaseId: String = UUID.randomUUID().toString(), val returnsToUnlockOrigin: Boolean = false) {
    val priceText: String get() = NumberFormat.getNumberInstance(Locale("vi", "VN")).format(creditPackage.priceVnd) + "\u0111"

    val transactionReference: String get() = purchaseId.take(8).uppercase(Locale.ROOT)

    fun putInto(intent: Intent): Intent = intent
        .putExtra(EXTRA_PACKAGE_ID, creditPackage.id)
        .putExtra(EXTRA_CREDITS, creditPackage.credits)
        .putExtra(EXTRA_PRICE, creditPackage.priceVnd)
        .putExtra(EXTRA_TITLE, creditPackage.title)
        .putExtra(EXTRA_DESCRIPTION, creditPackage.description)
        .putExtra(EXTRA_BALANCE, balanceCredits)
        .putExtra(EXTRA_PURCHASE_ID, purchaseId)
        .putExtra(EXTRA_RETURN_TO_ORIGIN, returnsToUnlockOrigin)

    companion object {
        private const val EXTRA_PACKAGE_ID = "credit_purchase_package_id"
        private const val EXTRA_CREDITS = "credit_purchase_credits"
        private const val EXTRA_PRICE = "credit_purchase_price"
        private const val EXTRA_TITLE = "credit_purchase_title"
        private const val EXTRA_DESCRIPTION = "credit_purchase_description"
        private const val EXTRA_BALANCE = "credit_purchase_balance"
        private const val EXTRA_PURCHASE_ID = "credit_purchase_id"
        private const val EXTRA_RETURN_TO_ORIGIN = "credit_purchase_return_to_origin"

        fun fromIntent(intent: Intent): CreditPurchaseSelection? {
            val id = intent.getStringExtra(EXTRA_PACKAGE_ID) ?: return null
            if (!intent.hasExtra(EXTRA_BALANCE) || !intent.hasExtra(EXTRA_CREDITS)) return null
            val selected = CreditPackage(
                id, intent.getIntExtra(EXTRA_CREDITS, 0), intent.getLongExtra(EXTRA_PRICE, 0),
                intent.getStringExtra(EXTRA_TITLE).orEmpty(), intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
            )
            return CreditPurchaseSelection(selected, intent.getIntExtra(EXTRA_BALANCE, 0), intent.getStringExtra(EXTRA_PURCHASE_ID) ?: UUID.randomUUID().toString(), intent.getBooleanExtra(EXTRA_RETURN_TO_ORIGIN, false))
        }
    }
}
