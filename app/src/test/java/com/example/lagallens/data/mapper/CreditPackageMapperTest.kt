package com.example.lagallens.data.mapper

import com.example.lagallens.data.model.response.CreditPackageResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditPackageMapperTest {
    private fun response(isRecommended: Boolean?) =
        CreditPackageResponse(2, "CREDITS_15", "15 Credits", "Phổ biến nhất", 15, 0, 79_000, 2, isRecommended)

    @Test fun theBackendRecommendationIsKept() {
        val offer = response(isRecommended = true).toCreditOffer()

        assertTrue(offer.isRecommended)
        assertEquals(79_000L, offer.priceVnd)
    }

    @Test fun aBackendWithoutTheFlagRecommendsNothing() {
        assertFalse(response(isRecommended = null).toCreditOffer().isRecommended)
    }
}
