package com.example.lagallens

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lagallens.data.datasource.local.datastore.CreditDemoLedger
import com.example.lagallens.data.repository.FakePaymentRepository
import com.example.lagallens.domain.model.payment.PaymentStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PaymentStatusSafetyTest {
    @Test fun directSuccessCannotSkipCreditAndCompletionIsIdempotent() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val ledger = CreditDemoLedger()
        val payments = FakePaymentRepository(context, ledger, "test_payment_${UUID.randomUUID()}")
        payments.createOrGet("payment", "package", 1000, 3)
        try { payments.updateStatus("payment", PaymentStatus.SUCCESS); fail("expected rejection") }
        catch (_: IllegalArgumentException) { }
        assertEquals(PaymentStatus.PENDING, payments.get("payment")?.status)
        assertEquals(PaymentStatus.SUCCESS, payments.completeDemoPayment("payment").status)
        payments.completeDemoPayment("payment")
        assertEquals(8, ledger.balance.value)
    }
}
