package com.example.lagallens.data.repository

import android.content.Context
import com.example.lagallens.R
import com.example.lagallens.domain.model.credit.CreditOffer

/** DEMO catalogue. Text is read on each call so it follows the active language. */
internal fun demoCreditOffers(context: Context): List<CreditOffer> = listOf(
    CreditOffer("credits_5", 5, 29_000, context.getString(R.string.credit_store_package_5_title), context.getString(R.string.credit_store_package_5_description)),
    CreditOffer("credits_15", 15, 79_000, context.getString(R.string.credit_store_package_15_title), context.getString(R.string.credit_store_package_15_description), isRecommended = true),
    CreditOffer("credits_30", 30, 149_000, context.getString(R.string.credit_store_package_30_title), context.getString(R.string.credit_store_package_30_description)),
    CreditOffer("credits_100", 100, 449_000, context.getString(R.string.credit_store_package_100_title), context.getString(R.string.credit_store_package_100_description))
)
