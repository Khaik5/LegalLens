package com.example.lagallens.data.datasource.local.datastore

import com.example.lagallens.data.repository.FinanceSnapshot
import com.example.lagallens.domain.model.payment.PaymentRecord
import com.example.lagallens.domain.model.payment.PaymentStatus
import org.junit.Assert.*
import org.junit.Test

class FinanceSnapshotCodecTest {
    @Test fun validSnapshotRoundTrips() {
        val original = FinanceSnapshot()
        assertEquals(original, FinanceSnapshotCodec.decode(FinanceSnapshotCodec.encode(original)))
    }
    @Test fun incompleteAndInconsistentWalletsAreRejected() {
        listOf("{}", "broken", FinanceSnapshotCodec.encode(FinanceSnapshot(balance = 0))).forEach { raw ->
            assertTrue(runCatching { FinanceSnapshotCodec.decode(raw) }.isFailure)
        }
    }
    @Test fun successfulPaymentWithoutLedgerCannotLoad() {
        val invalid = FinanceSnapshot(payments = mapOf("p" to PaymentRecord("p", "pack", 1000, 3, PaymentStatus.SUCCESS, 1)))
        assertTrue(runCatching { FinanceSnapshotCodec.decode(FinanceSnapshotCodec.encode(invalid)) }.isFailure)
    }
}
