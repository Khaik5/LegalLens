package com.example.lagallens.presentation.feature.creditstore.flow.ui

import android.content.Intent
import com.example.lagallens.presentation.feature.creditstore.contract.CreditPackage
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

internal data class CreditPurchaseSelection(val creditPackage: CreditPackage, val balanceCredits: Int, val purchaseId: String = UUID.randomUUID().toString()) {
    val priceText: String get() = NumberFormat.getNumberInstance(Locale("vi", "VN")).format(creditPackage.priceVnd) + "đ"

    fun putInto(intent: Intent): Intent = intent
        .putExtra(EXTRA_PACKAGE_ID, creditPackage.id)
        .putExtra(EXTRA_BALANCE, balanceCredits)
        .putExtra(EXTRA_PURCHASE_ID, purchaseId)

    companion object {
        private const val EXTRA_PACKAGE_ID = "credit_purchase_package_id"
        private const val EXTRA_BALANCE = "credit_purchase_balance"
        private const val EXTRA_PURCHASE_ID = "credit_purchase_id"

        fun fromIntent(intent: Intent): CreditPurchaseSelection? {
            val selected = CreditPackage.samples.firstOrNull { it.id == intent.getStringExtra(EXTRA_PACKAGE_ID) } ?: return null
            if (!intent.hasExtra(EXTRA_BALANCE)) return null
            return CreditPurchaseSelection(selected, intent.getIntExtra(EXTRA_BALANCE, 0), intent.getStringExtra(EXTRA_PURCHASE_ID) ?: UUID.randomUUID().toString())
        }
    }
}
