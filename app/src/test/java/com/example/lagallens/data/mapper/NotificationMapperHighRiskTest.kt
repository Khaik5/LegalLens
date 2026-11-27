package com.example.lagallens.data.mapper

import com.example.lagallens.data.model.response.NotificationResponse
import com.example.lagallens.domain.model.NotificationKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationMapperHighRiskTest {
    private fun response(type: String) = NotificationResponse(1, type, "message", false, 7, "2026-11-27T13:40:00Z")

    @Test fun highRiskFindingIsMarkedSoItIsNotShownAsAPlainCompletion() {
        val record = response("HIGH_RISK_FOUND").toNotification { "title" }
        assertEquals(NotificationKind.ANALYSIS, record.kind)
        assertTrue(record.isHighRisk)
    }

    @Test fun completedAnalysisIsNotHighRisk() {
        assertFalse(response("ANALYSIS_DONE").toNotification { "title" }.isHighRisk)
    }
}
