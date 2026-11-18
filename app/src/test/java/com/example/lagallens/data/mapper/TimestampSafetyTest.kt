package com.example.lagallens.data.mapper

import com.example.lagallens.data.model.response.*
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class TimestampSafetyTest {
    private val gson = Gson()
    private val valid = "2026-11-10T09:30:00.000Z"
    private val millis = 1_794_303_000_000L

    @Test fun contractListSurvivesMalformedAndGsonNullDates() {
        val rows = listOf("null", "\"invalid\"", "\"$valid\"").map { date ->
            gson.fromJson("""{"id":7,"title":"Contract","status":"ACTIVE","createdAt":$date,"updatedAt":$date}""", ContractResponse::class.java)
        }.map { it.toContract() }
        assertEquals(listOf(0L, 0L, millis), rows.map { it.createdAtMillis })
        assertEquals(listOf(0L, 0L, millis), rows.map { it.updatedAtMillis })
        assertEquals(listOf("7", "7", "7"), rows.map { it.id })
    }

    @Test fun documentListSurvivesMalformedAndGsonNullDates() {
        val rows = listOf("null", "\"invalid\"", "\"$valid\"").map { date ->
            gson.fromJson("""{"id":7,"contractId":3,"fileName":"a.pdf","fileType":"PDF","uploadedAt":$date}""", DocumentResponse::class.java)
        }.map { it.toDocument() }
        assertEquals(listOf(0L, 0L, millis), rows.map { it.updatedAtMillis })
        assertEquals(listOf("a.pdf", "a.pdf", "a.pdf"), rows.map { it.displayName })
    }

    @Test fun notificationListSurvivesMalformedAndGsonNullDates() {
        val rows = listOf("null", "\"invalid\"", "\"$valid\"").map { date ->
            gson.fromJson("""{"id":7,"message":"Hello","isRead":false,"createdAt":$date}""", NotificationResponse::class.java)
        }.map { it.toNotification { "Notice" } }
        assertEquals(listOf(0L, 0L, millis), rows.map { it.createdAtMillis })
        assertEquals(listOf("Hello", "Hello", "Hello"), rows.map { it.description })
    }

    @Test fun creditListSurvivesMalformedAndNullDates() {
        val rows = listOf(null, "invalid", valid).map {
            CreditTransactionResponse(7, "BONUS", 2.0, 5.0, null, null, it).toCreditTransaction()
        }
        assertEquals(listOf(0L, 0L, millis), rows.map { it.createdAtEpochMillis })
        assertEquals(listOf(2, 2, 2), rows.map { it.creditDelta })
    }
}
