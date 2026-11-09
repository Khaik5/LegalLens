package com.example.lagallens.domain.model.credit

/** A purchasable package. Its price and credits always come from the data source, never from the client. */
data class CreditOffer(
    val id: String,
    val credits: Int,
    val priceVnd: Long,
    val title: String,
    val description: String,
    val isRecommended: Boolean = false
)
